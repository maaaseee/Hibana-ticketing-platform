package com.aieta.ticketing_service.application.commands.confirm_reservation;

import com.aieta.ticketing_service.domain.enums.ReservationStatus;

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
