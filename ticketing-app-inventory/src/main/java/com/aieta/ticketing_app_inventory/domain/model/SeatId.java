package com.aieta.ticketing_app_inventory.domain.model;

import java.util.Objects;

public record SeatId(String value) {
    public SeatId {
        Objects.requireNonNull(value, "SeatId cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("SeatId cannot be blank");
        }
    }

    public static SeatId of(String value) {
        return new SeatId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
