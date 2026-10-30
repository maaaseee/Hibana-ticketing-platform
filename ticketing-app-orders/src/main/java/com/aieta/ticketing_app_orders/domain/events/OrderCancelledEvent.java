package com.aieta.ticketing_app_orders.domain.events;

import java.time.Instant;

public record OrderCancelledEvent(
        String orderId,
        String userId,
        String reservationId,
        String reason,
        Instant occurredAt
) {
}
