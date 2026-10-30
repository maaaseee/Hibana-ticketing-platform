package com.aieta.ticketing_app_orders.domain.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record OrderId(UUID value) implements Serializable {
    public OrderId {
        Objects.requireNonNull(value, "OrderId value cannot be null");
    }

    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }

    public static OrderId of(String raw) {
        Objects.requireNonNull(raw, "Raw OrderId string cannot be null");
        return new OrderId(UUID.fromString(raw));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
