package com.aieta.ticketing_app_orders.infrastructure.web.dto;

import com.aieta.ticketing_app_orders.domain.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record ProcessPaymentRequest(
        @NotNull(message = "paymentMethod is required")
        PaymentMethod paymentMethod,
        String buyerEmail,
        String buyerName,
        String buyerTaxId,
        Integer installments,
        String issuingBank,
        String eventTitle,
        String eventDate,
        String venueName,
        String venueAddress,
        String currency,
        BigDecimal subtotalAmount,
        BigDecimal serviceFeeAmount,
        BigDecimal vatTaxAmount,
        List<InvoiceLineItemDto> items
) {
    public record InvoiceLineItemDto(
            String description,
            String sectorAndSeat,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice
    ) {}

    public ProcessPaymentRequest(PaymentMethod paymentMethod) {
        this(paymentMethod, null, null, null, 1, "OTHER", null, null, null, null, null, null, null, null, null);
    }

    public ProcessPaymentRequest(PaymentMethod paymentMethod, String buyerEmail, String buyerName, String buyerTaxId) {
        this(paymentMethod, buyerEmail, buyerName, buyerTaxId, 1, "OTHER", null, null, null, null, null, null, null, null, null);
    }

    public ProcessPaymentRequest(PaymentMethod paymentMethod, String buyerEmail, String buyerName, String buyerTaxId,
                                 Integer installments, String issuingBank) {
        this(paymentMethod, buyerEmail, buyerName, buyerTaxId, installments, issuingBank, null, null, null, null, null, null, null, null, null);
    }
}
