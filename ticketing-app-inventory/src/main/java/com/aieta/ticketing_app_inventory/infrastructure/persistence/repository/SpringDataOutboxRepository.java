package com.aieta.ticketing_app_inventory.infrastructure.persistence.repository;

import com.aieta.ticketing_app_inventory.infrastructure.persistence.entity.OutboxEventJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SpringDataOutboxRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    @Query("SELECT o FROM OutboxEventJpaEntity o WHERE o.status = 'PENDING' ORDER BY o.createdAt ASC")
    List<OutboxEventJpaEntity> findPendingEvents(Pageable pageable);

    @Query(value = """
        SELECT * FROM outbox_events 
        WHERE status = 'PENDING' 
        ORDER BY created_at ASC 
        LIMIT :limit 
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<OutboxEventJpaEntity> findPendingEventsForUpdateSkipLocked(@Param("limit") int limit);
}
