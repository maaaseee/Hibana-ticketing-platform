package com.aieta.ticketing_app_catalog.domain.exceptions;

import java.math.BigDecimal;

public class InvalidSectorPricingException extends RuntimeException {
    public InvalidSectorPricingException(String message) {
        super(message);
    }

    public static InvalidSectorPricingException nonPositiveBasePrice(BigDecimal basePrice) {
        return new InvalidSectorPricingException("Base price must be strictly positive, but was: " + basePrice);
    }

    public static InvalidSectorPricingException negativeServiceFee(BigDecimal serviceFee) {
        return new InvalidSectorPricingException("Service fee cannot be negative, but was: " + serviceFee);
    }
}
