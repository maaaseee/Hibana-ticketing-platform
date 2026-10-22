package com.aieta.ticketing_app_inventory.application.queries.get_event_seats;

import java.util.List;

public record EventSeatsResponse(
        String eventId,
        int totalSeats,
        int availableSeats,
        int heldSeats,
        int bookedSeats,
        int blockedSeats,
        List<SeatResponseDto> seats
) {
}
