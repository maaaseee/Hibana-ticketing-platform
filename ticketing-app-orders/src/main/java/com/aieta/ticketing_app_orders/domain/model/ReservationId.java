package com.aieta.ticketing_app_orders.domain.model;

import java.io.Serializable;
import java.util.Objects;

public record ReservationId(String value) implements Serializable {
    public ReservationId {
        Objects.requireNonNull(value, "ReservationId cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("ReservationId cannot be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
