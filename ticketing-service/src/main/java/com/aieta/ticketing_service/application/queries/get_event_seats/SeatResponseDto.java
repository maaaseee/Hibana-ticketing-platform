package com.aieta.ticketing_service.application.queries.get_event_seats;

import com.aieta.ticketing_service.domain.enums.SeatStatus;

import java.math.BigDecimal;

public record SeatResponseDto(
        String seatId,
        String eventId,
        String sectorId,
        String rowIdentifier,
        String seatNumber,
        BigDecimal price,
        String currency,
        SeatStatus status
) {
}
