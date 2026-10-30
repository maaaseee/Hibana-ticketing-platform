package com.aieta.ticketing_app_orders.infrastructure.persistence.repository;

import com.aieta.ticketing_app_orders.infrastructure.persistence.entity.VirtualWalletJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataWalletRepository extends JpaRepository<VirtualWalletJpaEntity, UUID> {
    Optional<VirtualWalletJpaEntity> findByUserId(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM VirtualWalletJpaEntity w WHERE w.userId = :userId")
    Optional<VirtualWalletJpaEntity> findByUserIdForUpdate(@Param("userId") String userId);
}
