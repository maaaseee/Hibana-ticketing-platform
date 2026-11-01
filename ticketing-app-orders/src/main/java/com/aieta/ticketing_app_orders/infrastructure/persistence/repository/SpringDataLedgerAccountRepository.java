package com.aieta.ticketing_app_orders.infrastructure.persistence.repository;

import com.aieta.ticketing_app_orders.infrastructure.persistence.entity.LedgerAccountJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataLedgerAccountRepository extends JpaRepository<LedgerAccountJpaEntity, UUID> {
    Optional<LedgerAccountJpaEntity> findByOwnerIdAndAccountType(String ownerId, String accountType);
}
