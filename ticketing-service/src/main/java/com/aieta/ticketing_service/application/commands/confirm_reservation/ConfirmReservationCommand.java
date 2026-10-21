package com.aieta.ticketing_service.application.commands.confirm_reservation;

import com.aieta.ticketing_service.application.common.mediator.Request;

import java.util.Objects;

public record ConfirmReservationCommand(
        String reservationId,
        String orderId
) implements Request<ConfirmationResult> {
    public ConfirmReservationCommand {
        Objects.requireNonNull(reservationId, "reservationId cannot be null");
        Objects.requireNonNull(orderId, "orderId cannot be null");
    }
}
