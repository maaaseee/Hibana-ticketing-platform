package com.aieta.ticketing_app_inventory.domain.exceptions;

import java.time.Instant;

public class ReservationExpiredException extends DomainException {
    private final String reservationId;
    private final Instant expiredAt;

    public ReservationExpiredException(String reservationId, Instant expiredAt) {
        super(String.format("Reservation '%s' has expired at %s", reservationId, expiredAt));
        this.reservationId = reservationId;
        this.expiredAt = expiredAt;
    }

    public String getReservationId() {
        return reservationId;
    }

    public Instant getExpiredAt() {
        return expiredAt;
    }
}
