package com.aieta.ticketing_app_orders.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProcessPaymentResponse(
        String orderId,
        String status,
        String paymentMethod,
        BigDecimal totalAmount,
        Instant paidAt
) {
}
