package com.aieta.ticketing_app_catalog.domain.port;

import com.aieta.ticketing_app_catalog.domain.entity.Event;

import java.util.Optional;

public interface EventWriteRepositoryPort {
    Event saveWithOutbox(Event event, Object domainEvent);
    Optional<Event> findByIdForUpdate(String id);
    boolean existsBySlug(String slug);
    boolean venueExists(String venueId);
}
