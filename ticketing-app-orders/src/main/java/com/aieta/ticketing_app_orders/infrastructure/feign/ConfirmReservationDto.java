package com.aieta.ticketing_app_orders.infrastructure.feign;

import java.time.Instant;

public record ConfirmReservationDto(
        String reservationId,
        String eventId,
        String seatId,
        String status,
        String orderId,
        Instant confirmedAt
) {}
