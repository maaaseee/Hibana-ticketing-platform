package com.aieta.ticketing_service.application.commands.generate_seats;

import java.math.BigDecimal;
import java.util.Objects;

public record SectorDefinition(
        String sectorId,
        String name,
        String type,
        int totalCapacity,
        BigDecimal basePrice,
        BigDecimal serviceFee,
        String currency
) {
    public SectorDefinition {
        Objects.requireNonNull(sectorId, "sectorId cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        if (totalCapacity <= 0) {
            throw new IllegalArgumentException("totalCapacity must be greater than 0");
        }
        if (basePrice == null) basePrice = BigDecimal.ZERO;
        if (serviceFee == null) serviceFee = BigDecimal.ZERO;
        if (currency == null || currency.isBlank()) currency = "USD";
    }
}
