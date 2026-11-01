package com.aieta.ticketing_app_catalog.infrastructure.database.adapter;

import com.aieta.ticketing_app_catalog.domain.entity.EventSummary;
import com.aieta.ticketing_app_catalog.domain.port.EventRepositoryPort;
import com.aieta.ticketing_app_catalog.infrastructure.database.mapper.EventMapper;
import com.aieta.ticketing_app_catalog.infrastructure.database.repository.SpringDataEventRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class JpaEventRepositoryAdapter implements EventRepositoryPort {

    private final SpringDataEventRepository eventRepository;
    private final EventMapper eventMapper;

    public JpaEventRepositoryAdapter(SpringDataEventRepository eventRepository, EventMapper eventMapper) {
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
    }

    @Override
    public List<EventSummary> findUpcomingPublished() {
        return eventRepository.findUpcomingPublished()
                .stream()
                .map(e -> eventMapper.toDomain(e))
                .toList();
    }

    @Override
    public List<EventSummary> findByCategoryAndPublished(String category) {
        return eventRepository.findByCategoryAndPublished(category)
                .stream()
                .map(e -> eventMapper.toDomain(e))
                .toList();
    }

    @Override
    public List<EventSummary> findRecentlyAddedPublished() {
        return eventRepository.findRecentlyAddedPublished()
                .stream()
                .map(e -> eventMapper.toDomain(e))
                .toList();
    }

    @Override
    public List<EventSummary> findUpcomingPublishedTop3(Instant referenceDate) {
        Instant ref = referenceDate != null ? referenceDate : Instant.now();
        return eventRepository.findUpcomingPublishedTop3(ref)
                .stream()
                .map(e -> eventMapper.toDomain(e))
                .toList();
    }

    @Override
    public Optional<Instant> findFurthestPublishedEventDate() {
        return eventRepository.findFurthestPublishedEventDate();
    }

    @Override
    public List<EventSummary> searchEventsFuzzy(String query, String venue) {
        return eventRepository.searchEventsFuzzy(query, venue)
                .stream()
                .map(e -> eventMapper.toDomain(e))
                .toList();
    }

    @Override
    public Optional<EventSummary> findById(String id) {
        return eventRepository.findById(id).map(e -> eventMapper.toDomain(e));
    }

    @Override
    public Optional<EventSummary> findBySlug(String slug) {
        return eventRepository.findBySlug(slug).map(e -> eventMapper.toDomain(e));
    }
}
