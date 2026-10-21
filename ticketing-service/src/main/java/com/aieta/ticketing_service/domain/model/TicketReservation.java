package com.aieta.ticketing_service.domain.model;

import com.aieta.ticketing_service.domain.enums.ReservationStatus;
import com.aieta.ticketing_service.domain.exceptions.InvalidSeatStateTransitionException;
import com.aieta.ticketing_service.domain.exceptions.ReservationExpiredException;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public class TicketReservation {
    private final ReservationId id;
    private final String eventId;
    private final SeatId seatId;
    private final String userId;
    private ReservationStatus status;
    private final Instant expiresAt;
    private final Instant createdAt;
    private Instant confirmedAt;
    private String orderId;

    private TicketReservation(
            ReservationId id,
            String eventId,
            SeatId seatId,
            String userId,
            ReservationStatus status,
            Instant expiresAt,
            Instant createdAt,
            Instant confirmedAt,
            String orderId
    ) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.eventId = Objects.requireNonNull(eventId, "eventId cannot be null");
        this.seatId = Objects.requireNonNull(seatId, "seatId cannot be null");
        this.userId = Objects.requireNonNull(userId, "userId cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt cannot be null");
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.confirmedAt = confirmedAt;
        this.orderId = orderId;
    }

    public static TicketReservation createNew(
            String eventId,
            SeatId seatId,
            String userId,
            Duration holdDuration
    ) {
        Instant now = Instant.now();
        Duration duration = (holdDuration != null && !holdDuration.isNegative() && !holdDuration.isZero())
                ? holdDuration
                : Duration.ofSeconds(600);
        Instant expiresAt = now.plus(duration);
        return new TicketReservation(
                ReservationId.generate(),
                eventId,
                seatId,
                userId,
                ReservationStatus.PENDING_PAYMENT,
                expiresAt,
                now,
                null,
                null
        );
    }

    public static TicketReservation reconstitute(
            ReservationId id,
            String eventId,
            SeatId seatId,
            String userId,
            ReservationStatus status,
            Instant expiresAt,
            Instant createdAt,
            Instant confirmedAt,
            String orderId
    ) {
        return new TicketReservation(id, eventId, seatId, userId, status, expiresAt, createdAt, confirmedAt, orderId);
    }

    public void confirm(String orderId) {
        confirm(orderId, Instant.now());
    }

    public void confirm(String orderId, Instant now) {
        Objects.requireNonNull(orderId, "orderId cannot be null");
        if (this.status != ReservationStatus.PENDING_PAYMENT) {
            throw new InvalidSeatStateTransitionException(
                    String.format("Cannot confirm reservation '%s' with current status '%s'", this.id.value(), this.status)
            );
        }
        if (isExpired(now)) {
            this.status = ReservationStatus.EXPIRED;
            throw new ReservationExpiredException(this.id.value(), this.expiresAt);
        }
        this.status = ReservationStatus.CONFIRMED;
        this.orderId = orderId;
        this.confirmedAt = now;
    }

    public void cancel() {
        if (this.status == ReservationStatus.CONFIRMED) {
            throw new InvalidSeatStateTransitionException(
                    String.format("Cannot cancel an already CONFIRMED reservation '%s'", this.id.value())
            );
        }
        this.status = ReservationStatus.CANCELLED;
    }

    public void expire() {
        this.status = ReservationStatus.EXPIRED;
    }

    public boolean isExpired() {
        return isExpired(Instant.now());
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(this.expiresAt);
    }

    public ReservationId getId() { return id; }
    public String getEventId() { return eventId; }
    public SeatId getSeatId() { return seatId; }
    public String getUserId() { return userId; }
    public ReservationStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public String getOrderId() { return orderId; }
}
