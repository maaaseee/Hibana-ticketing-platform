package com.aieta.ticketing_app_inventory.application.commands.confirm_reservation;

import com.aieta.ticketing_app_inventory.domain.enums.ReservationStatus;

import java.time.Instant;

public record ConfirmationResult(
        String reservationId,
        String eventId,
        String seatId,
        ReservationStatus status,
        String orderId,
        Instant confirmedAt
) {
}
