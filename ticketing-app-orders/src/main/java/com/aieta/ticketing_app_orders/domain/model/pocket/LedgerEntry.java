package com.aieta.ticketing_app_orders.domain.model.pocket;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record LedgerEntry(
        UUID id,
        UUID accountId,
        EntryType entryType,
        BigDecimal amount,
        UUID transactionRef,
        String description,
        Instant postedAt
) {
    public enum EntryType {
        DEBIT,
        CREDIT
    }

    public LedgerEntry {
        Objects.requireNonNull(id, "Entry id cannot be null");
        Objects.requireNonNull(accountId, "Account id cannot be null");
        Objects.requireNonNull(entryType, "Entry type cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(transactionRef, "Transaction reference cannot be null");
        postedAt = postedAt != null ? postedAt : Instant.now();
    }

    public static LedgerEntry debit(UUID accountId, BigDecimal amount, UUID transactionRef, String description) {
        return new LedgerEntry(UUID.randomUUID(), accountId, EntryType.DEBIT, amount, transactionRef, description, Instant.now());
    }

    public static LedgerEntry credit(UUID accountId, BigDecimal amount, UUID transactionRef, String description) {
        return new LedgerEntry(UUID.randomUUID(), accountId, EntryType.CREDIT, amount, transactionRef, description, Instant.now());
    }
}
