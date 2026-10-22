package com.aieta.ticketing_app_inventory.infrastructure.web.dto;

import com.aieta.ticketing_app_inventory.domain.enums.ReservationStatus;

import java.time.Instant;

public record ConfirmReservationResponse(
        String reservationId,
        String eventId,
        String seatId,
        ReservationStatus status,
        String orderId,
        Instant confirmedAt
) {
}
