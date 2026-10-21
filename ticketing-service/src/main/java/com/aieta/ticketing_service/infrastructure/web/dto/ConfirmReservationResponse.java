package com.aieta.ticketing_service.infrastructure.web.dto;

import com.aieta.ticketing_service.domain.enums.ReservationStatus;

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
