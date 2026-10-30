package com.aieta.ticketing_app_orders.application.commands.process_payment;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResult(
        String orderId,
        String status,
        String paymentMethod,
        BigDecimal totalAmount,
        Instant paidAt
) {
}
