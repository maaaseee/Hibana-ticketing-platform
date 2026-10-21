package com.aieta.ticketing_app_catalog.domain.entity;

import com.aieta.ticketing_app_catalog.domain.exceptions.InvalidSectorCapacityException;
import com.aieta.ticketing_app_catalog.domain.exceptions.InvalidSectorPricingException;

import java.math.BigDecimal;

public record EventSectorSummary(
        String sectorId,
        String name,
        String type,
        int totalCapacity,
        BigDecimal basePrice,
        BigDecimal serviceFee,
        String currency
) {
    public EventSectorSummary {
        if (totalCapacity <= 0) {
            throw new InvalidSectorCapacityException(totalCapacity);
        }
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw InvalidSectorPricingException.nonPositiveBasePrice(basePrice);
        }
        if (serviceFee == null || serviceFee.compareTo(BigDecimal.ZERO) < 0) {
            throw InvalidSectorPricingException.negativeServiceFee(serviceFee);
        }
    }
}
