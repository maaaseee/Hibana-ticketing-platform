package com.aieta.ticketing_app_orders.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class OrderItem {
    private final UUID id;
    private final String eventId;
    private final String seatId;
    private final String sectorId;
    private final BigDecimal unitPrice;
    private final int quantity;
    private final String description;

    public OrderItem(UUID id, String eventId, String seatId, String sectorId, BigDecimal unitPrice, int quantity, String description) {
        this.id = id != null ? id : UUID.randomUUID();
        this.eventId = Objects.requireNonNull(eventId, "eventId cannot be null");
        this.seatId = Objects.requireNonNull(seatId, "seatId cannot be null");
        this.sectorId = sectorId;
        this.unitPrice = Objects.requireNonNull(unitPrice, "unitPrice cannot be null");
        this.quantity = quantity > 0 ? quantity : 1;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getSeatId() {
        return seatId;
    }

    public String getSectorId() {
        return sectorId;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getDescription() {
        return description;
    }
}
