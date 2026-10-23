package com.aieta.ticketing_app_catalog.domain.entity;

import com.aieta.ticketing_app_catalog.domain.exceptions.InvalidSectorCapacityException;
import com.aieta.ticketing_app_catalog.domain.exceptions.InvalidSectorException;
import com.aieta.ticketing_app_catalog.domain.exceptions.InvalidSectorPricingException;

import java.math.BigDecimal;

public class EventSector {

    private final String id;
    private final String name;
    private final String type;
    private final int totalCapacity;
    private final BigDecimal basePrice;
    private final BigDecimal serviceFee;
    private final String currency;

    public EventSector(String id, String name, String type, int totalCapacity, BigDecimal basePrice, BigDecimal serviceFee, String currency) {
        if (id == null || id.isBlank()) {
            throw new InvalidSectorException("Sector ID cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new InvalidSectorException("Sector name cannot be null or blank");
        }
        if (totalCapacity <= 0) {
            throw new InvalidSectorCapacityException(totalCapacity);
        }
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw InvalidSectorPricingException.nonPositiveBasePrice(basePrice);
        }
        if (serviceFee == null || serviceFee.compareTo(BigDecimal.ZERO) < 0) {
            throw InvalidSectorPricingException.negativeServiceFee(serviceFee);
        }

        this.id = id;
        this.name = name.trim();
        this.type = type != null && !type.isBlank() ? type.trim() : "GENERAL_ADMISSION";
        this.totalCapacity = totalCapacity;
        this.basePrice = basePrice;
        this.serviceFee = serviceFee;
        this.currency = currency != null && !currency.isBlank() ? currency.trim().toUpperCase() : "ARS";
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public int getTotalCapacity() { return totalCapacity; }
    public BigDecimal getBasePrice() { return basePrice; }
    public BigDecimal getServiceFee() { return serviceFee; }
    public String getCurrency() { return currency; }
}
