package com.aieta.ticketing_app_inventory.infrastructure.web.dto;

import com.aieta.ticketing_app_inventory.domain.enums.ReservationStatus;

import java.time.Instant;

public record HoldSeatResponse(
        String reservationId,
        String eventId,
        String seatId,
        String userId,
        ReservationStatus status,
        Instant expiresAt,
        long countdownSeconds
) {
}
