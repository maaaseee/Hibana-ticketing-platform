package com.aieta.ticketing_app_catalog.domain.port;

import com.aieta.ticketing_app_catalog.domain.entity.EventSummary;

import java.util.List;
import java.util.Optional;

public interface EventRepositoryPort {
    List<EventSummary> findUpcomingPublished();
    List<EventSummary> findByCategoryAndPublished(String category);
    Optional<EventSummary> findById(String id);
    Optional<EventSummary> findBySlug(String slug);
}
