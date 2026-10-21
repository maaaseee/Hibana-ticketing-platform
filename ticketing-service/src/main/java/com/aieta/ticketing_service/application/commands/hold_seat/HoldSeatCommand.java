package com.aieta.ticketing_service.application.commands.hold_seat;

import com.aieta.ticketing_service.application.common.mediator.Request;

import java.time.Duration;
import java.util.Objects;

public record HoldSeatCommand(
        String eventId,
        String seatId,
        String userId,
        Duration holdDuration
) implements Request<ReservationResult> {
    public HoldSeatCommand {
        Objects.requireNonNull(eventId, "eventId cannot be null");
        Objects.requireNonNull(seatId, "seatId cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        if (holdDuration == null) {
            holdDuration = Duration.ofSeconds(600);
        }
    }

    public HoldSeatCommand(String eventId, String seatId, String userId) {
        this(eventId, seatId, userId, Duration.ofSeconds(600));
    }
}
