package com.aieta.ticketing_app_catalog.infrastructure.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Ensures that the mock Azure Blob Storage container and sample venue seat map layout
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
            seedEstadioNacionalSeatMap();
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

    private void seedEstadioNacionalSeatMap() {
        try {
            String blobName = "estadio-nacional-seatmap.json";
            String blobUrl = blobEndpoint + "/" + venuesContainer + "/" + blobName;

            String layoutJson = """
            {
              "venueId": "ven_estadio_nacional",
              "venueName": "Estadio Nacional Julio Martínez Prádanos",
              "canvas": {
                "width": 1920,
                "height": 1080
              },
              "sectors": [
                {
                  "sectorId": "sec_coldplay_vip",
                  "name": "Cancha VIP Front",
                  "type": "GENERAL_ADMISSION",
                  "polygon": [[500, 300], [1420, 300], [1420, 600], [500, 600]],
                  "color": "#4A90E2"
                },
                {
                  "sectorId": "sec_coldplay_andes",
                  "name": "Tribuna Andes",
                  "type": "NUMBERED_SEATS",
                  "polygon": [[200, 200], [450, 200], [450, 800], [200, 800]],
                  "color": "#E94E77",
                  "rows": 40,
                  "seatsPerRow": 30
                }
              ]
            }
            """;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(blobUrl))
                    .header("x-ms-blob-type", "BlockBlob")
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(layoutJson, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(3))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201 || response.statusCode() == 200) {
                log.info("Successfully provisioned venue seat map blob on Floci-AZ: {}", blobUrl);
            } else {
                log.info("Seat map blob upload returned status: {}", response.statusCode());
            }
        } catch (Exception e) {
            log.debug("Floci-AZ seed blob: {}", e.getMessage());
        }
    }
}
