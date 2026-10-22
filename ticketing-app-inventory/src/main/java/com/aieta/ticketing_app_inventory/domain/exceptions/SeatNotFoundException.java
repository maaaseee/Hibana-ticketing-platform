package com.aieta.ticketing_app_inventory.domain.exceptions;

public class SeatNotFoundException extends DomainException {
    private final String seatId;

    public SeatNotFoundException(String seatId) {
        super(String.format("Seat '%s' was not found in the inventory", seatId));
        this.seatId = seatId;
    }

    public SeatNotFoundException(String eventId, String seatId) {
        super(String.format("Seat '%s' for event '%s' was not found in the inventory", seatId, eventId));
        this.seatId = seatId;
    }

    public String getSeatId() {
        return seatId;
    }
}
