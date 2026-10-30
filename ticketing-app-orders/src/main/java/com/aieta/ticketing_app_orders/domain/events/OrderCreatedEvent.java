package com.aieta.ticketing_app_orders.domain.events;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderCreatedEvent(
        String orderId,
        String userId,
        String reservationId,
        String eventId,
        String seatId,
        BigDecimal total,
        Instant occurredAt
) {
}
