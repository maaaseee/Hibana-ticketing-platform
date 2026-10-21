package com.aieta.ticketing_app_catalog.infrastructure.database.adapter;

import com.aieta.ticketing_app_catalog.domain.entity.EventSummary;
import com.aieta.ticketing_app_catalog.domain.port.EventRepositoryPort;
import com.aieta.ticketing_app_catalog.infrastructure.database.mapper.EventMapper;
import com.aieta.ticketing_app_catalog.infrastructure.database.repository.SpringDataEventRepository;
import org.springframework.stereotype.Component;

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
                .map(eventMapper::toDomain)
                .toList();
    }

    @Override
    public List<EventSummary> findByCategoryAndPublished(String category) {
        return eventRepository.findByCategoryAndPublished(category)
                .stream()
                .map(eventMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<EventSummary> findById(String id) {
        return eventRepository.findById(id).map(eventMapper::toDomain);
    }

    @Override
    public Optional<EventSummary> findBySlug(String slug) {
        return eventRepository.findBySlug(slug).map(eventMapper::toDomain);
    }
}
