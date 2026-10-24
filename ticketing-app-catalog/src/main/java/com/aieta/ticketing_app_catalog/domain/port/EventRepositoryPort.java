package com.aieta.ticketing_app_catalog.domain.port;

import com.aieta.ticketing_app_catalog.domain.entity.EventSummary;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EventRepositoryPort {
    List<EventSummary> findUpcomingPublished();
    List<EventSummary> findByCategoryAndPublished(String category);
    List<EventSummary> findRecentlyAddedPublished();
    List<EventSummary> findUpcomingPublishedTop3(Instant referenceDate);
    Optional<Instant> findFurthestPublishedEventDate();
    List<EventSummary> searchEventsFuzzy(String query, String venue);
    Optional<EventSummary> findById(String id);
    Optional<EventSummary> findBySlug(String slug);
}
