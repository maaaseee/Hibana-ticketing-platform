package com.aieta.ticketing_app_inventory.domain.exceptions;

public class ReservationNotFoundException extends DomainException {
    private final String reservationId;

    public ReservationNotFoundException(String reservationId) {
        super(String.format("Reservation '%s' was not found", reservationId));
        this.reservationId = reservationId;
    }

    public String getReservationId() {
        return reservationId;
    }
}
