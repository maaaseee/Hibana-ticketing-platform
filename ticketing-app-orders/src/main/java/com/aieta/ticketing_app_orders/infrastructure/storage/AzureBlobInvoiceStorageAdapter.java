package com.aieta.ticketing_app_orders.infrastructure.storage;

import com.aieta.ticketing_app_orders.application.ports.InvoiceStoragePort;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AzureBlobInvoiceStorageAdapter implements InvoiceStoragePort {

    private static final Logger log = LoggerFactory.getLogger(AzureBlobInvoiceStorageAdapter.class);

    private final boolean enabled;
    private final String connectionString;
    private final String containerName;

    private BlobContainerClient containerClient;
    private final Map<String, byte[]> fallbackStore = new ConcurrentHashMap<>();

    public AzureBlobInvoiceStorageAdapter(
            @Value("${azure.storage.blob.enabled:false}") boolean enabled,
            @Value("${azure.storage.blob.connection-string:}") String connectionString,
            @Value("${azure.storage.blob.container-name:hibana-invoices}") String containerName
    ) {
        this.enabled = enabled;
        this.connectionString = connectionString;
        this.containerName = containerName;
    }

    @PostConstruct
    public void init() {
        if (!enabled || connectionString == null || connectionString.isBlank()) {
            log.info("[AZURE-BLOB-STORAGE] Azure Blob Storage is disabled or unconfigured; using in-memory invoice fallback store.");
            return;
        }

        try {
            log.info("[AZURE-BLOB-STORAGE] Initializing BlobServiceClient for container [{}]", containerName);
            BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();

            containerClient = serviceClient.getBlobContainerClient(containerName);
            if (!containerClient.exists()) {
                containerClient.create();
                log.info("[AZURE-BLOB-STORAGE] Created blob container [{}]", containerName);
            } else {
                log.info("[AZURE-BLOB-STORAGE] Blob container [{}] exists and is ready.", containerName);
            }
        } catch (Exception e) {
            log.warn("[AZURE-BLOB-STORAGE] Failed to initialize Azure Blob container [{}]: {}. Falling back to in-memory store.",
                    containerName, e.getMessage());
            containerClient = null;
        }
    }

    @Override
    public String storeInvoicePdf(String orderId, byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            return null;
        }

        fallbackStore.put(orderId, pdfBytes);

        if (containerClient != null) {
            try {
                String blobName = "invoices/" + orderId + ".pdf";
                BlobClient blobClient = containerClient.getBlobClient(blobName);

                BlobHttpHeaders headers = new BlobHttpHeaders()
                        .setContentType("application/pdf");

                blobClient.upload(BinaryData.fromBytes(pdfBytes), true);
                blobClient.setHttpHeaders(headers);

                log.info("[AZURE-BLOB-STORAGE] Stored invoice PDF for order [{}] in blob [{}] ({} bytes)",
                        orderId, blobName, pdfBytes.length);
                return blobClient.getBlobUrl();
            } catch (Exception e) {
                log.error("[AZURE-BLOB-STORAGE] Error uploading invoice PDF for order [{}] to Azure Blob: {}", orderId, e.getMessage(), e);
            }
        }

        return "memory://invoices/" + orderId + ".pdf";
    }

    @Override
    public Optional<byte[]> retrieveInvoicePdf(String orderId) {
        if (containerClient != null) {
            try {
                String blobName = "invoices/" + orderId + ".pdf";
                BlobClient blobClient = containerClient.getBlobClient(blobName);
                if (blobClient.exists()) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    blobClient.downloadStream(out);
                    byte[] bytes = out.toByteArray();
                    log.info("[AZURE-BLOB-STORAGE] Successfully retrieved invoice PDF for order [{}] from blob [{}] ({} bytes)",
                            orderId, blobName, bytes.length);
                    return Optional.of(bytes);
                }
            } catch (Exception e) {
                log.warn("[AZURE-BLOB-STORAGE] Error retrieving invoice PDF from Azure Blob for order [{}]: {}", orderId, e.getMessage());
            }
        }

        byte[] fallback = fallbackStore.get(orderId);
        if (fallback != null) {
            return Optional.of(fallback);
        }

        return Optional.empty();
    }
}
