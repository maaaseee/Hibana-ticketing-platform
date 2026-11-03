package com.aieta.ticketing_app_catalog.infrastructure.database.repository;

import com.aieta.ticketing_app_catalog.infrastructure.database.entity.EventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SpringDataEventRepository extends JpaRepository<EventJpaEntity, String> {

    @Query("SELECT e FROM EventJpaEntity e WHERE e.status = 'PUBLISHED' AND e.startsAt > CURRENT_TIMESTAMP ORDER BY e.startsAt ASC")
    List<EventJpaEntity> findUpcomingPublished();

    @Query("SELECT e FROM EventJpaEntity e WHERE e.status = 'PUBLISHED' AND e.category = :category AND e.startsAt > CURRENT_TIMESTAMP ORDER BY e.startsAt ASC")
    List<EventJpaEntity> findByCategoryAndPublished(@Param("category") String category);

    @Query("SELECT e FROM EventJpaEntity e WHERE e.status = 'PUBLISHED' ORDER BY e.createdAt DESC LIMIT 3")
    List<EventJpaEntity> findRecentlyAddedPublished();

    @Query("SELECT e FROM EventJpaEntity e WHERE e.status = 'PUBLISHED' AND e.startsAt >= :referenceDate ORDER BY e.startsAt ASC LIMIT 3")
    List<EventJpaEntity> findUpcomingPublishedTop3(@Param("referenceDate") Instant referenceDate);

    @Query("SELECT MAX(e.startsAt) FROM EventJpaEntity e WHERE e.status = 'PUBLISHED' AND e.startsAt > CURRENT_TIMESTAMP")
    Optional<Instant> findFurthestPublishedEventDate();

    @Query(value = """
        SELECT e.* FROM events e
        JOIN venues v ON e.venue_id = v.id
        WHERE e.status = 'PUBLISHED'
          AND e.starts_at > CURRENT_TIMESTAMP
          AND (:venue IS NULL OR :venue = '' OR :venue = 'all' OR e.venue_id = :venue OR LOWER(v.name) = LOWER(:venue))
          AND (
            :query IS NULL OR :query = '' OR
            LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%')) OR
            word_similarity(LOWER(:query), LOWER(e.title)) > 0.3
          )
        ORDER BY
          CASE
            WHEN :query IS NULL OR :query = '' THEN 1.0
            WHEN LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%')) THEN 1.0
            ELSE word_similarity(LOWER(:query), LOWER(e.title))
          END DESC,
          e.starts_at ASC
        """, nativeQuery = true)
    List<EventJpaEntity> searchEventsFuzzy(@Param("query") String query, @Param("venue") String venue);

    Optional<EventJpaEntity> findBySlug(String slug);

    @Query("SELECT e FROM EventJpaEntity e ORDER BY e.createdAt DESC")
    List<EventJpaEntity> findAllOrderByCreatedAtDesc();

    @Query("SELECT e FROM EventJpaEntity e WHERE e.status = :status ORDER BY e.createdAt DESC")
    List<EventJpaEntity> findByStatusOrderByCreatedAtDesc(@Param("status") String status);
}
