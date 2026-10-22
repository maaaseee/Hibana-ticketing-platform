package com.aieta.ticketing_app_inventory.domain.events;

import java.time.Instant;
import java.util.Objects;

public record ReservationConfirmedEvent(
        String reservationId,
        String eventId,
        String seatId,
        String orderId,
        Instant confirmedAt
) {
    public ReservationConfirmedEvent {
        Objects.requireNonNull(reservationId, "reservationId cannot be null");
        Objects.requireNonNull(eventId, "eventId cannot be null");
        Objects.requireNonNull(seatId, "seatId cannot be null");
        Objects.requireNonNull(orderId, "orderId cannot be null");
        Objects.requireNonNull(confirmedAt, "confirmedAt cannot be null");
    }

    public static ReservationConfirmedEvent of(String reservationId, String eventId, String seatId, String orderId) {
        return new ReservationConfirmedEvent(reservationId, eventId, seatId, orderId, Instant.now());
    }
}
