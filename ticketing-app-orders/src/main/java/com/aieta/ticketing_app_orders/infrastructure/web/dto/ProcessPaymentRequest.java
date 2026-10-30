package com.aieta.ticketing_app_orders.infrastructure.web.dto;

import com.aieta.ticketing_app_orders.domain.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record ProcessPaymentRequest(
        @NotNull(message = "paymentMethod is required")
        PaymentMethod paymentMethod
) {
}
