package com.aieta.ticketing_app_orders.domain.model.pocket;

import com.aieta.ticketing_app_orders.domain.exceptions.LedgerImbalanceException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class DoubleEntryTransaction {
    private final UUID transactionRef;
    private final List<LedgerEntry> entries = new ArrayList<>();

    public DoubleEntryTransaction(UUID transactionRef) {
        this.transactionRef = transactionRef != null ? transactionRef : UUID.randomUUID();
    }

    public DoubleEntryTransaction addDebit(UUID accountId, BigDecimal amount, String description) {
        entries.add(LedgerEntry.debit(accountId, amount, transactionRef, description));
        return this;
    }

    public DoubleEntryTransaction addCredit(UUID accountId, BigDecimal amount, String description) {
        entries.add(LedgerEntry.credit(accountId, amount, transactionRef, description));
        return this;
    }

    public List<LedgerEntry> buildAndValidate() {
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }

        BigDecimal totalDebits = entries.stream()
                .filter(e -> e.entryType() == LedgerEntry.EntryType.DEBIT)
                .map(e -> e.amount())
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        BigDecimal totalCredits = entries.stream()
                .filter(e -> e.entryType() == LedgerEntry.EntryType.CREDIT)
                .map(e -> e.amount())
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new LedgerImbalanceException(totalDebits, totalCredits);
        }

        return Collections.unmodifiableList(entries);
    }

    public UUID getTransactionRef() {
        return transactionRef;
    }
}
