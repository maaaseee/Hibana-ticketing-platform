package com.aieta.ticketing_app_orders.domain.exceptions;

import java.math.BigDecimal;

public class InsufficientFundsException extends DomainException {
    private final BigDecimal available;
    private final BigDecimal required;

    public InsufficientFundsException(BigDecimal available, BigDecimal required) {
        super("Insufficient wallet funds. Required: " + required + ", Available: " + available);
        this.available = available;
        this.required = required;
    }

    public BigDecimal getAvailable() {
        return available;
    }

    public BigDecimal getRequired() {
        return required;
    }
}
