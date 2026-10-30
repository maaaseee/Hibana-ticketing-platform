package com.aieta.ticketing_app_orders.domain.events;

import java.time.Instant;

public record PaymentFailedEvent(
        String orderId,
        String userId,
        String reservationId,
        String eventId,
        String seatId,
        String reason,
        Instant occurredAt
) {
}
