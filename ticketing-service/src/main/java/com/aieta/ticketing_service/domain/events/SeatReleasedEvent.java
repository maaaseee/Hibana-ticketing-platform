package com.aieta.ticketing_service.domain.events;

import java.time.Instant;
import java.util.Objects;

public record SeatReleasedEvent(
        String seatId,
        String eventId,
        String reason,
        Instant occurredAt
) {
    public SeatReleasedEvent {
        Objects.requireNonNull(seatId, "seatId cannot be null");
        Objects.requireNonNull(eventId, "eventId cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }

    public static SeatReleasedEvent of(String seatId, String eventId, String reason) {
        return new SeatReleasedEvent(seatId, eventId, reason, Instant.now());
    }
}
