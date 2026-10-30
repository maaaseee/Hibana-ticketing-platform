package com.aieta.ticketing_app_orders.infrastructure.persistence.repository;

import com.aieta.ticketing_app_orders.infrastructure.persistence.entity.LedgerEntryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataLedgerEntryRepository extends JpaRepository<LedgerEntryJpaEntity, UUID> {
    List<LedgerEntryJpaEntity> findByAccountIdOrderByPostedAtAsc(UUID accountId);
    List<LedgerEntryJpaEntity> findByTransactionRef(UUID transactionRef);
}
