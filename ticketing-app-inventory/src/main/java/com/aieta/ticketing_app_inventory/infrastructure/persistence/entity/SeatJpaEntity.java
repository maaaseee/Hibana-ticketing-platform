package com.aieta.ticketing_app_inventory.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "seats", uniqueConstraints = {
        @UniqueConstraint(name = "uq_seat_coordinates", columnNames = {"event_id", "sector_id", "row_identifier", "seat_number"})
}, indexes = {
        @Index(name = "idx_seats_event_status", columnList = "event_id, status"),
        @Index(name = "idx_seats_sector", columnList = "sector_id")
})
public class SeatJpaEntity {

    @Id
    @Column(name = "id", length = 60, nullable = false)
    private String id;

    @Column(name = "event_id", length = 60, nullable = false)
    private String eventId;

    @Column(name = "sector_id", length = 60, nullable = false)
    private String sectorId;

    @Column(name = "row_identifier", length = 20, nullable = false)
    private String rowIdentifier;

    @Column(name = "seat_number", length = 20, nullable = false)
    private String seatNumber;

    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "currency", length = 10, nullable = false)
    private String currency = "ARS";

    @Column(name = "status", length = 30, nullable = false)
    private String status = "AVAILABLE";

    @Version
    @Column(name = "version", nullable = false)
    private long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public SeatJpaEntity() {}

    public SeatJpaEntity(String id, String eventId, String sectorId, String rowIdentifier, String seatNumber, BigDecimal price, String currency, String status, long version, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.eventId = eventId;
        this.sectorId = sectorId;
        this.rowIdentifier = rowIdentifier;
        this.seatNumber = seatNumber;
        this.price = price;
        this.currency = currency;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getSectorId() { return sectorId; }
    public void setSectorId(String sectorId) { this.sectorId = sectorId; }
    public String getRowIdentifier() { return rowIdentifier; }
    public void setRowIdentifier(String rowIdentifier) { this.rowIdentifier = rowIdentifier; }
    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
