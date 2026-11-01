package com.aieta.ticketing_app_orders.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record InvoiceData(
        String invoiceNumber,
        String orderId,
        Instant issuedAt,
        String caeNumber,
        Instant caeExpiresAt,
        String buyerName,
        String buyerEmail,
        String buyerTaxId,
        String paymentMethod,
        String eventId,
        String eventTitle,
        String eventDate,
        String venueName,
        String venueAddress,
        List<InvoiceItem> items,
        BigDecimal subtotal,
        BigDecimal serviceFee,
        BigDecimal vatTax,
        BigDecimal grandTotal,
        String currency
) {
    public record InvoiceItem(
            String description,
            String sectorAndSeat,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice
    ) {}
}
