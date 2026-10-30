package com.aieta.ticketing_app_orders.domain.events;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderPaidEvent(
        String orderId,
        String userId,
        String reservationId,
        String eventId,
        String seatId,
        BigDecimal total,
        String paymentMethod,
        Instant occurredAt
) {
}
