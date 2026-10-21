package com.aieta.ticketing_service.domain.exceptions;

public class SeatAlreadyHeldException extends DomainException {
    private final String eventId;
    private final String seatId;

    public SeatAlreadyHeldException(String eventId, String seatId) {
        super(String.format("Seat '%s' for event '%s' is already held or reserved", seatId, eventId));
        this.eventId = eventId;
        this.seatId = seatId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getSeatId() {
        return seatId;
    }
}
