package com.aieta.ticketing_app_inventory.application.commands.hold_seat;

import com.aieta.ticketing_app_inventory.domain.enums.ReservationStatus;

import java.time.Instant;

public record ReservationResult(
        String reservationId,
        String eventId,
        String seatId,
        String userId,
        ReservationStatus status,
        Instant expiresAt,
        long countdownSeconds
) {
}
