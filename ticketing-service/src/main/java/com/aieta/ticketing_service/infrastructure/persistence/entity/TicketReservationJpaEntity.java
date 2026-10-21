package com.aieta.ticketing_service.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ticket_reservations", indexes = {
        @Index(name = "idx_reservations_user", columnList = "user_id"),
        @Index(name = "idx_reservations_event_seat", columnList = "event_id, seat_id"),
        @Index(name = "idx_reservations_status_expires", columnList = "status, expires_at")
})
public class TicketReservationJpaEntity {

    @Id
    @Column(name = "id", length = 60, nullable = false)
    private String id;

    @Column(name = "event_id", length = 60, nullable = false)
    private String eventId;

    @Column(name = "seat_id", length = 60, nullable = false)
    private String seatId;

    @Column(name = "user_id", length = 60, nullable = false)
    private String userId;

    @Column(name = "status", length = 30, nullable = false)
    private String status = "PENDING_PAYMENT";

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "order_id", length = 60)
    private String orderId;

    public TicketReservationJpaEntity() {}

    public TicketReservationJpaEntity(String id, String eventId, String seatId, String userId, String status, Instant expiresAt, Instant createdAt, Instant confirmedAt, String orderId) {
        this.id = id;
        this.eventId = eventId;
        this.seatId = seatId;
        this.userId = userId;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.confirmedAt = confirmedAt;
        this.orderId = orderId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getSeatId() { return seatId; }
    public void setSeatId(String seatId) { this.seatId = seatId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
}
