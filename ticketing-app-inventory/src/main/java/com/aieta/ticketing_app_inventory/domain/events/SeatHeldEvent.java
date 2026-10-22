package com.aieta.ticketing_app_inventory.domain.events;

import java.time.Instant;
import java.util.Objects;

public record SeatHeldEvent(
        String reservationId,
        String eventId,
        String seatId,
        String userId,
        Instant expiresAt,
        Instant occurredAt
) {
    public SeatHeldEvent {
        Objects.requireNonNull(reservationId, "reservationId cannot be null");
        Objects.requireNonNull(eventId, "eventId cannot be null");
        Objects.requireNonNull(seatId, "seatId cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(expiresAt, "expiresAt cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }

    public static SeatHeldEvent of(String reservationId, String eventId, String seatId, String userId, Instant expiresAt) {
        return new SeatHeldEvent(reservationId, eventId, seatId, userId, expiresAt, Instant.now());
    }
}
