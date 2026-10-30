package com.aieta.ticketing_app_orders.domain.ports;

import com.aieta.ticketing_app_orders.domain.model.pocket.LedgerAccount;
import com.aieta.ticketing_app_orders.domain.model.pocket.LedgerEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LedgerRepositoryPort {
    LedgerAccount saveAccount(LedgerAccount account);
    Optional<LedgerAccount> findAccountById(UUID id);
    Optional<LedgerAccount> findAccountByOwnerIdAndType(String ownerId, LedgerAccount.AccountType type);
    void recordEntries(List<LedgerEntry> entries);
}
