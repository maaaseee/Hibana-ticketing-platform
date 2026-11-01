package com.aieta.ticketing_app_catalog.infrastructure.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Ensures that the mock Azure Blob Storage container and sample venue seat map layouts
 * are provisioned on the Floci-AZ emulator (http://localhost:4577/devstoreaccount1).
 */
@Component
@Order(10)
public class FlociBlobStorageInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FlociBlobStorageInitializer.class);

    private final String blobEndpoint;
    private final String venuesContainer;
    private final HttpClient httpClient;

    public FlociBlobStorageInitializer(
            @Value("${catalog.azure.storage.blob.endpoint:http://localhost:4577/devstoreaccount1}") String blobEndpoint,
            @Value("${catalog.azure.storage.blob.container:venues}") String venuesContainer) {
        this.blobEndpoint = blobEndpoint.endsWith("/") ? blobEndpoint.substring(0, blobEndpoint.length() - 1) : blobEndpoint;
        this.venuesContainer = venuesContainer;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureContainerExists();
            seedAllVenueSeatMaps();
            seedEstadioNacionalSeatMap();
            seedVenueImages();
        } catch (Exception e) {
            log.warn("Floci-AZ Blob Storage initialization skipped or unavailable: {}", e.getMessage());
        }
    }

    private void ensureContainerExists() {
        try {
            String url = blobEndpoint + "/" + venuesContainer + "?restype=container";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(3))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201 || response.statusCode() == 200 || response.statusCode() == 409) {
                log.info("Floci-AZ container [{}] verified/created (HTTP {})", venuesContainer, response.statusCode());
            } else {
                log.info("Floci-AZ container create returned status: {}", response.statusCode());
            }
        } catch (Exception e) {
            log.debug("Floci-AZ ensure container: {}", e.getMessage());
        }
    }

    private void seedAllVenueSeatMaps() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:seat-maps/*.json");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) continue;
                try (InputStream is = resource.getInputStream()) {
                    String jsonContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    uploadBlob(filename, jsonContent);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to seed classpath seat-maps to Floci-AZ: {}", e.getMessage());
        }
    }

    private void seedVenueImages() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:venue-images/*.*");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) continue;
                String contentType = filename.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
                try (InputStream is = resource.getInputStream()) {
                    byte[] bytes = is.readAllBytes();
                    uploadBinaryBlob(filename, bytes, contentType);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to seed classpath venue-images to Floci-AZ: {}", e.getMessage());
        }
    }

    private void uploadBinaryBlob(String blobName, byte[] data, String contentType) {
        try {
            String blobUrl = blobEndpoint + "/" + venuesContainer + "/" + blobName;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(blobUrl))
                    .header("x-ms-blob-type", "BlockBlob")
                    .header("Content-Type", contentType)
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(data))
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201 || response.statusCode() == 200) {
                log.info("Provisioned venue image [{}] on Floci-AZ: {}", blobName, blobUrl);
            }
        } catch (Exception e) {
            log.debug("Floci-AZ uploadBinaryBlob [{}]: {}", blobName, e.getMessage());
        }
    }

    private void uploadBlob(String blobName, String jsonContent) {
        try {
            String blobUrl = blobEndpoint + "/" + venuesContainer + "/" + blobName;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(blobUrl))
                    .header("x-ms-blob-type", "BlockBlob")
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonContent, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(3))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201 || response.statusCode() == 200) {
                log.info("Provisioned venue seat map [{}] on Floci-AZ: {}", blobName, blobUrl);
            }
        } catch (Exception e) {
            log.debug("Floci-AZ uploadBlob [{}]: {}", blobName, e.getMessage());
        }
    }

    private void seedEstadioNacionalSeatMap() {
        try {
            String blobName = "estadio-nacional-seatmap.json";

            String layoutJson = """
            {
              "venueId": "ven_estadio_nacional",
              "venueName": "Estadio Nacional Julio Martínez Prádanos",
              "canvasWidth": 1920,
              "canvasHeight": 1080,
              "sectors": [
                {
                  "id": "sec_coldplay_vip",
                  "name": "Cancha VIP Front",
                  "type": "GENERAL_ADMISSION",
                  "shapeType": "POLYGON",
                  "points": [[500, 300], [1420, 300], [1420, 600], [500, 600]],
                  "capacity": 5000
                },
                {
                  "id": "sec_coldplay_andes",
                  "name": "Tribuna Andes",
                  "type": "NUMBERED_SEATS",
                  "shapeType": "POLYGON",
                  "points": [[200, 200], [450, 200], [450, 800], [200, 800]],
                  "capacity": 12000
                }
              ]
            }
            """;

            uploadBlob(blobName, layoutJson);
        } catch (Exception e) {
            log.debug("Floci-AZ seed blob: {}", e.getMessage());
        }
    }
}
