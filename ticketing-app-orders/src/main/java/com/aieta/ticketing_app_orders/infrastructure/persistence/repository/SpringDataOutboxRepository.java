package com.aieta.ticketing_app_orders.infrastructure.persistence.repository;

import com.aieta.ticketing_app_orders.infrastructure.persistence.entity.OutboxEventJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataOutboxRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    @Query(value = "SELECT * FROM orders.outbox_events WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT :batchSize FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEventJpaEntity> findPendingEventsForUpdateSkipLocked(@Param("batchSize") int batchSize);

    @Query("SELECT o FROM OutboxEventJpaEntity o WHERE o.status = 'PENDING' ORDER BY o.createdAt ASC")
    List<OutboxEventJpaEntity> findPendingEvents(Pageable pageable);
}
