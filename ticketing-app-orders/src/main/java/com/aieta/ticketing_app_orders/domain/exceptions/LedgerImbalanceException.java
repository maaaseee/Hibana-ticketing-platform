package com.aieta.ticketing_app_orders.domain.exceptions;

import java.math.BigDecimal;

public class LedgerImbalanceException extends DomainException {
    private static final long serialVersionUID = 1L;

    private final BigDecimal totalDebits;
    private final BigDecimal totalCredits;

    public LedgerImbalanceException(BigDecimal totalDebits, BigDecimal totalCredits) {
        super("Ledger double-entry imbalance detected. Total Debits: " + totalDebits + ", Total Credits: " + totalCredits);
        this.totalDebits = totalDebits;
        this.totalCredits = totalCredits;
    }

    public BigDecimal getTotalDebits() {
        return totalDebits;
    }

    public BigDecimal getTotalCredits() {
        return totalCredits;
    }
}
