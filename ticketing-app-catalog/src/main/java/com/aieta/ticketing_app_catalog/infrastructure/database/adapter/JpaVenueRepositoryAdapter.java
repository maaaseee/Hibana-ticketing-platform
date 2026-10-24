package com.aieta.ticketing_app_catalog.infrastructure.database.adapter;

import com.aieta.ticketing_app_catalog.domain.entity.VenueSummary;
import com.aieta.ticketing_app_catalog.domain.port.VenueRepositoryPort;
import com.aieta.ticketing_app_catalog.infrastructure.database.mapper.EventMapper;
import com.aieta.ticketing_app_catalog.infrastructure.database.repository.SpringDataVenueRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JpaVenueRepositoryAdapter implements VenueRepositoryPort {

    private final SpringDataVenueRepository venueRepository;
    private final EventMapper eventMapper;

    public JpaVenueRepositoryAdapter(SpringDataVenueRepository venueRepository, EventMapper eventMapper) {
        this.venueRepository = venueRepository;
        this.eventMapper = eventMapper;
    }

    @Override
    public List<VenueSummary> findAll() {
        return venueRepository.findAll().stream()
                .map(eventMapper::toVenueSummary)
                .toList();
    }
}
