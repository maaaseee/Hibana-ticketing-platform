package com.aieta.ticketing_app_orders.application.ports;

import java.util.Optional;

public interface InvoiceStoragePort {

    /**
     * Stores the binary PDF invoice data for the given order.
     *
     * @param orderId the order identifier
     * @param pdfBytes binary PDF bytes
     * @return the URI or storage reference of the stored invoice
     */
    String storeInvoicePdf(String orderId, byte[] pdfBytes);

    /**
     * Retrieves the stored binary PDF invoice data for the given order if available.
     *
     * @param orderId the order identifier
     * @return Optional containing the PDF bytes, or empty if not found
     */
    Optional<byte[]> retrieveInvoicePdf(String orderId);
}
