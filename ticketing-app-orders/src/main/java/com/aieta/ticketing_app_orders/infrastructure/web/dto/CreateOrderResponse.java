package com.aieta.ticketing_app_orders.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateOrderResponse(
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
