package com.aieta.ticketing_app_inventory.application.commands.release_seat;

import com.aieta.ticketing_app_inventory.application.common.mediator.Request;

import java.util.Objects;

public record ReleaseSeatCommand(
        String eventId,
        String seatId,
        String reason
) implements Request<Void> {
    public ReleaseSeatCommand {
        Objects.requireNonNull(eventId, "eventId cannot be null");
        Objects.requireNonNull(seatId, "seatId cannot be null");
        if (reason == null || reason.isBlank()) {
            reason = "USER_CANCELLED";
        }
    }
}
