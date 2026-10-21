package com.aieta.ticketing_app_catalog.infrastructure.database.adapter;

import com.aieta.ticketing_app_catalog.domain.entity.Event;
import com.aieta.ticketing_app_catalog.domain.exceptions.DuplicateEventSlugException;
import com.aieta.ticketing_app_catalog.domain.exceptions.VenueNotFoundException;
import com.aieta.ticketing_app_catalog.domain.port.EventWriteRepositoryPort;
import com.aieta.ticketing_app_catalog.infrastructure.database.entity.EventJpaEntity;
import com.aieta.ticketing_app_catalog.infrastructure.database.entity.OutboxEventJpaEntity;
import com.aieta.ticketing_app_catalog.infrastructure.database.entity.VenueJpaEntity;
import com.aieta.ticketing_app_catalog.infrastructure.database.mapper.EventMapper;
import com.aieta.ticketing_app_catalog.infrastructure.database.repository.SpringDataEventRepository;
import com.aieta.ticketing_app_catalog.infrastructure.database.repository.SpringDataOutboxEventRepository;
import com.aieta.ticketing_app_catalog.infrastructure.database.repository.SpringDataVenueRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaEventWriteRepositoryAdapter implements EventWriteRepositoryPort {

    private final SpringDataEventRepository eventRepository;
    private final SpringDataVenueRepository venueRepository;
    private final SpringDataOutboxEventRepository outboxRepository;
    private final EventMapper eventMapper;
    private final ObjectMapper objectMapper;

    public JpaEventWriteRepositoryAdapter(
            SpringDataEventRepository eventRepository,
            SpringDataVenueRepository venueRepository,
            SpringDataOutboxEventRepository outboxRepository,
            EventMapper eventMapper,
            ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.outboxRepository = outboxRepository;
        this.eventMapper = eventMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean existsBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return false;
        }
        return eventRepository.findBySlug(slug.trim()).isPresent();
    }

    @Override
    public boolean venueExists(String venueId) {
        if (venueId == null || venueId.isBlank()) {
            return false;
        }
        return venueRepository.existsById(venueId.trim());
    }

    @Override
    public Optional<Event> findByIdForUpdate(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return eventRepository.findById(id.trim()).map(eventMapper::toDomainAggregate);
    }

    @Override
    @Transactional
    public Event saveWithOutbox(Event event, Object domainEvent) {
        VenueJpaEntity venue = venueRepository.findById(event.getVenueId())
                .orElseThrow(() -> new VenueNotFoundException(event.getVenueId()));

        EventJpaEntity entity = eventMapper.toJpaEntity(event, venue);
        EventJpaEntity saved;
        try {
            saved = eventRepository.saveAndFlush(entity);
        } catch (org.springframework.dao.DataIntegrityViolationException dive) {
            throw new DuplicateEventSlugException(event.getSlug());
        }

        String payload;
        try {
            payload = objectMapper.writeValueAsString(domainEvent);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize domain event: " + e.getMessage(), e);
        }

        String eventType = domainEvent.getClass().getSimpleName();
        OutboxEventJpaEntity outbox = new OutboxEventJpaEntity(
                UUID.randomUUID(),
                "Event",
                saved.getId(),
                eventType,
                payload
        );
        outboxRepository.save(outbox);

        return eventMapper.toDomainAggregate(saved);
    }
}
