package com.aieta.ticketing_app_orders.infrastructure.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AzureBlobInvoiceStorageAdapterTest {

    @Test
    @DisplayName("Should store and retrieve invoice PDF from fallback store when Azure Blob is disabled")
    void shouldStoreAndRetrieveInvoiceFromFallbackStore() {
        AzureBlobInvoiceStorageAdapter adapter = new AzureBlobInvoiceStorageAdapter(false, "", "hibana-invoices");
        adapter.init();

        String orderId = "order-test-123";
        byte[] pdfContent = "%PDF-1.4 Mock Invoice Content".getBytes(StandardCharsets.UTF_8);

        String ref = adapter.storeInvoicePdf(orderId, pdfContent);
        assertThat(ref).isNotNull().startsWith("memory://");

        Optional<byte[]> retrieved = adapter.retrieveInvoicePdf(orderId);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get()).isEqualTo(pdfContent);
    }

    @Test
    @DisplayName("Should return empty Optional when invoice does not exist")
    void shouldReturnEmptyWhenInvoiceNotFound() {
        AzureBlobInvoiceStorageAdapter adapter = new AzureBlobInvoiceStorageAdapter(false, "", "hibana-invoices");
        adapter.init();

        Optional<byte[]> retrieved = adapter.retrieveInvoicePdf("nonexistent-order");
        assertThat(retrieved).isEmpty();
    }
}
