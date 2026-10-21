package com.aieta.ticketing_app_catalog.infrastructure.database.repository;

import com.aieta.ticketing_app_catalog.infrastructure.database.entity.EventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataEventRepository extends JpaRepository<EventJpaEntity, String> {

    @Query("SELECT e FROM EventJpaEntity e WHERE e.status = 'PUBLISHED' AND e.startsAt > CURRENT_TIMESTAMP ORDER BY e.startsAt ASC")
    List<EventJpaEntity> findUpcomingPublished();

    @Query("SELECT e FROM EventJpaEntity e WHERE e.status = 'PUBLISHED' AND e.category = :category AND e.startsAt > CURRENT_TIMESTAMP ORDER BY e.startsAt ASC")
    List<EventJpaEntity> findByCategoryAndPublished(@Param("category") String category);

    Optional<EventJpaEntity> findBySlug(String slug);
}
