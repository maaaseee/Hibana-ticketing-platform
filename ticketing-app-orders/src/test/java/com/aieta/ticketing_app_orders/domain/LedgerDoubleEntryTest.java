package com.aieta.ticketing_app_orders.domain;

import com.aieta.ticketing_app_orders.domain.exceptions.LedgerImbalanceException;
import com.aieta.ticketing_app_orders.domain.model.pocket.DoubleEntryTransaction;
import com.aieta.ticketing_app_orders.domain.model.pocket.LedgerAccount;
import com.aieta.ticketing_app_orders.domain.model.pocket.LedgerEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LedgerDoubleEntryTest {

    @Test
    @DisplayName("Should validate balanced double entry transaction")
    void shouldValidateBalancedTransaction() {
        UUID txRef = UUID.randomUUID();
        UUID buyerWalletAccountId = UUID.randomUUID();
        UUID platformRevenueAccountId = UUID.randomUUID();

        DoubleEntryTransaction tx = new DoubleEntryTransaction(txRef)
                .addDebit(platformRevenueAccountId, new BigDecimal("50.00"), "Ticket purchase revenue")
                .addCredit(buyerWalletAccountId, new BigDecimal("50.00"), "User wallet debit deduction");

        List<LedgerEntry> entries = tx.buildAndValidate();
        assertEquals(2, entries.size());
    }

    @Test
    @DisplayName("Should reject unbalanced transaction where debits != credits")
    void shouldRejectImbalancedTransaction() {
        UUID txRef = UUID.randomUUID();
        UUID accountA = UUID.randomUUID();
        UUID accountB = UUID.randomUUID();

        DoubleEntryTransaction tx = new DoubleEntryTransaction(txRef)
                .addDebit(accountA, new BigDecimal("50.00"), "Debit entry")
                .addCredit(accountB, new BigDecimal("49.99"), "Unequal credit entry");

        assertThrows(LedgerImbalanceException.class, tx::buildAndValidate);
    }

    @Test
    @DisplayName("Should compute correct account balance based on debits and credits")
    void shouldComputeCorrectBalance() {
        UUID accountId = UUID.randomUUID();
        UUID txRef = UUID.randomUUID();
        LedgerAccount assetAccount = new LedgerAccount(accountId, "PLATFORM", LedgerAccount.AccountType.ASSET, BigDecimal.ZERO, Currency.getInstance("EUR"));

        List<LedgerEntry> entries = List.of(
                LedgerEntry.debit(accountId, new BigDecimal("100.00"), txRef, "Deposit"),
                LedgerEntry.credit(accountId, new BigDecimal("30.00"), txRef, "Withdrawal")
        );

        BigDecimal balance = assetAccount.computeBalance(entries);
        assertEquals(new BigDecimal("70.00"), balance);
    }
}
