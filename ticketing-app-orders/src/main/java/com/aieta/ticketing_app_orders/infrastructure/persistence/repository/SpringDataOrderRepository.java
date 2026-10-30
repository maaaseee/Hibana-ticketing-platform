package com.aieta.ticketing_app_orders.infrastructure.persistence.repository;

import com.aieta.ticketing_app_orders.infrastructure.persistence.entity.OrderJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {
    Optional<OrderJpaEntity> findByReservationId(String reservationId);
    List<OrderJpaEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    List<OrderJpaEntity> findByStatusAndCreatedAtBefore(String status, Instant cutoff);
}
