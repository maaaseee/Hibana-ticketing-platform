package com.aieta.ticketing_app_orders.infrastructure.persistence.adapter;

import com.aieta.ticketing_app_orders.domain.model.pocket.LedgerAccount;
import com.aieta.ticketing_app_orders.domain.model.pocket.LedgerEntry;
import com.aieta.ticketing_app_orders.domain.ports.LedgerRepositoryPort;
import com.aieta.ticketing_app_orders.infrastructure.persistence.entity.LedgerAccountJpaEntity;
import com.aieta.ticketing_app_orders.infrastructure.persistence.entity.LedgerEntryJpaEntity;
import com.aieta.ticketing_app_orders.infrastructure.persistence.mapper.LedgerMapper;
import com.aieta.ticketing_app_orders.infrastructure.persistence.repository.SpringDataLedgerAccountRepository;
import com.aieta.ticketing_app_orders.infrastructure.persistence.repository.SpringDataLedgerEntryRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaLedgerRepositoryAdapter implements LedgerRepositoryPort {

    private final SpringDataLedgerAccountRepository accountRepository;
    private final SpringDataLedgerEntryRepository entryRepository;

    public JpaLedgerRepositoryAdapter(SpringDataLedgerAccountRepository accountRepository,
                                      SpringDataLedgerEntryRepository entryRepository) {
        this.accountRepository = accountRepository;
        this.entryRepository = entryRepository;
    }

    @Override
    public LedgerAccount saveAccount(LedgerAccount account) {
        LedgerAccountJpaEntity entity = LedgerMapper.toEntityAccount(account);
        LedgerAccountJpaEntity saved = accountRepository.save(entity);
        return LedgerMapper.toDomainAccount(saved);
    }

    @Override
    public Optional<LedgerAccount> findAccountById(UUID id) {
        return accountRepository.findById(id)
                .map(a -> LedgerMapper.toDomainAccount(a));
    }

    @Override
    public Optional<LedgerAccount> findAccountByOwnerIdAndType(String ownerId, LedgerAccount.AccountType type) {
        return accountRepository.findByOwnerIdAndAccountType(ownerId, type.name())
                .map(a -> LedgerMapper.toDomainAccount(a));
    }

    @Override
    public void recordEntries(List<LedgerEntry> entries) {
        List<LedgerEntryJpaEntity> entities = entries.stream()
                .map(e -> LedgerMapper.toEntityEntry(e))
                .toList();
        entryRepository.saveAll(entities);
    }
}
