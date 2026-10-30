package com.aieta.ticketing_app_orders.application.commands.create_order;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateOrderResult(
        String orderId,
        String userId,
        String reservationId,
        String eventId,
        String seatId,
        BigDecimal totalAmount,
        String status,
        Instant createdAt
) {
}
