package com.aieta.ticketing_app_orders.infrastructure.web.dto;

import com.aieta.ticketing_app_orders.domain.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record ProcessPaymentRequest(
        @NotNull(message = "paymentMethod is required")
        PaymentMethod paymentMethod,
        String buyerEmail,
        String buyerName,
        String buyerTaxId
) {
    public ProcessPaymentRequest(PaymentMethod paymentMethod) {
        this(paymentMethod, null, null, null);
    }
}
