package com.aieta.ticketing_app_inventory.application.commands.generate_seats;

import java.math.BigDecimal;
import java.util.Objects;

public record SectorDefinition(
        String sectorId,
        String name,
        String type,
        int totalCapacity,
        BigDecimal basePrice,
        BigDecimal serviceFee,
        String currency,
        Integer seatsPerRow
) {
    public SectorDefinition(String sectorId, String name, String type, int totalCapacity, BigDecimal basePrice, BigDecimal serviceFee, String currency) {
        this(sectorId, name, type, totalCapacity, basePrice, serviceFee, currency, null);
    }

    public SectorDefinition {
        Objects.requireNonNull(sectorId, "sectorId cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        if (totalCapacity <= 0) {
            throw new IllegalArgumentException("totalCapacity must be greater than 0");
        }
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("basePrice must be greater than 0");
        }
        if (serviceFee == null || serviceFee.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("serviceFee must be greater than 0");
        }
        if (currency == null || currency.isBlank()) {
            currency = "ARS";
        } else if (!"ARS".equalsIgnoreCase(currency.trim())) {
            throw new IllegalArgumentException("Only ARS currency is supported, but received: " + currency);
        } else {
            currency = "ARS";
        }
        if (seatsPerRow != null && seatsPerRow <= 0) {
            throw new IllegalArgumentException("seatsPerRow must be greater than 0 if specified");
        }
    }
}
