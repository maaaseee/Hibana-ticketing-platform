package com.aieta.ticketing_app_inventory.domain.model;

import com.aieta.ticketing_app_inventory.domain.enums.SeatStatus;
import com.aieta.ticketing_app_inventory.domain.exceptions.InvalidSeatStateTransitionException;
import com.aieta.ticketing_app_inventory.domain.exceptions.SeatAlreadyHeldException;

import java.time.Instant;
import java.util.Objects;

public class Seat {
    private final SeatId id;
    private final String eventId;
    private final String sectorId;
    private final String rowIdentifier;
    private final String seatNumber;
    private Money price;
    private SeatStatus status;
    private long version;
    private final Instant createdAt;
    private Instant updatedAt;

    private Seat(SeatId id,
                 String eventId,
                 String sectorId,
                 String rowIdentifier,
                 String seatNumber,
                 Money price,
                 SeatStatus status,
                 long version,
                 Instant createdAt,
                 Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.eventId = Objects.requireNonNull(eventId, "eventId cannot be null");
        this.sectorId = Objects.requireNonNull(sectorId, "sectorId cannot be null");
        this.rowIdentifier = Objects.requireNonNull(rowIdentifier, "rowIdentifier cannot be null");
        this.seatNumber = Objects.requireNonNull(seatNumber, "seatNumber cannot be null");
        this.price = Objects.requireNonNull(price, "price cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.version = version;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static Seat createNew(
            SeatId id,
            String eventId,
            String sectorId,
            String rowIdentifier,
            String seatNumber,
            Money price
    ) {
        Instant now = Instant.now();
        return new Seat(id, eventId, sectorId, rowIdentifier, seatNumber, price, SeatStatus.AVAILABLE, 0L, now, now);
    }

    public static Seat reconstitute(
            SeatId id,
            String eventId,
            String sectorId,
            String rowIdentifier,
            String seatNumber,
            Money price,
            SeatStatus status,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Seat(id, eventId, sectorId, rowIdentifier, seatNumber, price, status, version, createdAt, updatedAt);
    }

    public void hold(String userId) {
        if (this.status == SeatStatus.HELD) {
            throw new SeatAlreadyHeldException(this.eventId, this.id.value());
        }
        if (this.status == SeatStatus.BOOKED || this.status == SeatStatus.BLOCKED) {
            throw new InvalidSeatStateTransitionException(this.status, SeatStatus.HELD);
        }
        this.status = SeatStatus.HELD;
        this.updatedAt = Instant.now();
    }

    public void book() {
        if (this.status != SeatStatus.HELD) {
            throw new InvalidSeatStateTransitionException(this.status, SeatStatus.BOOKED);
        }
        this.status = SeatStatus.BOOKED;
        this.updatedAt = Instant.now();
    }

    public void release() {
        if (this.status == SeatStatus.BOOKED) {
            throw new InvalidSeatStateTransitionException(
                    String.format("Cannot release a BOOKED seat '%s' directly", this.id.value())
            );
        }
        this.status = SeatStatus.AVAILABLE;
        this.updatedAt = Instant.now();
    }

    public void block() {
        if (this.status != SeatStatus.AVAILABLE) {
            throw new InvalidSeatStateTransitionException(
                    String.format("Cannot block seat '%s' in state '%s'", this.id.value(), this.status)
            );
        }
        this.status = SeatStatus.BLOCKED;
        this.updatedAt = Instant.now();
    }

    public void unblock() {
        if (this.status != SeatStatus.BLOCKED) {
            throw new InvalidSeatStateTransitionException(
                    String.format("Cannot unblock seat '%s' in state '%s'", this.id.value(), this.status)
            );
        }
        this.status = SeatStatus.AVAILABLE;
        this.updatedAt = Instant.now();
    }

    public boolean isAvailable() {
        return this.status == SeatStatus.AVAILABLE;
    }

    public boolean isHeld() {
        return this.status == SeatStatus.HELD;
    }

    public boolean isBooked() {
        return this.status == SeatStatus.BOOKED;
    }

    public SeatId getId() { return id; }
    public String getEventId() { return eventId; }
    public String getSectorId() { return sectorId; }
    public String getRowIdentifier() { return rowIdentifier; }
    public String getSeatNumber() { return seatNumber; }
    public Money getPrice() { return price; }
    public SeatStatus getStatus() { return status; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
