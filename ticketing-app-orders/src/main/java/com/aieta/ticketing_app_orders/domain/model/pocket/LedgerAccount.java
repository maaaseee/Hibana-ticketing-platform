package com.aieta.ticketing_app_orders.domain.model.pocket;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record LedgerAccount(
        UUID id,
        String ownerId,
        AccountType type,
        BigDecimal balance,
        Currency currency
) {
    public enum AccountType {
        ASSET,
        LIABILITY,
        EQUITY,
        REVENUE,
        EXPENSE
    }

    public LedgerAccount {
        Objects.requireNonNull(id, "Account id cannot be null");
        Objects.requireNonNull(ownerId, "Owner id cannot be null");
        Objects.requireNonNull(type, "Account type cannot be null");
        balance = balance != null ? balance : BigDecimal.ZERO;
        currency = currency != null ? currency : Currency.getInstance("EUR");
    }

    public BigDecimal computeBalance(List<LedgerEntry> entries) {
        BigDecimal debits = entries.stream()
                .filter(e -> e.entryType() == LedgerEntry.EntryType.DEBIT)
                .map(LedgerEntry::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal credits = entries.stream()
                .filter(e -> e.entryType() == LedgerEntry.EntryType.CREDIT)
                .map(LedgerEntry::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return switch (type) {
            case ASSET, EXPENSE -> debits.subtract(credits);
            case LIABILITY, EQUITY, REVENUE -> credits.subtract(debits);
        };
    }
}
