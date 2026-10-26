package com.aieta.ticketing_app_catalog.infrastructure.database.mapper;

import com.aieta.ticketing_app_catalog.domain.entity.Event;
import com.aieta.ticketing_app_catalog.domain.entity.EventSectorSummary;
import com.aieta.ticketing_app_catalog.domain.entity.EventStatus;
import com.aieta.ticketing_app_catalog.domain.entity.EventSummary;
import com.aieta.ticketing_app_catalog.domain.entity.VenueSummary;
import com.aieta.ticketing_app_catalog.infrastructure.database.entity.EventJpaEntity;
import com.aieta.ticketing_app_catalog.infrastructure.database.entity.EventSectorJpaEntity;
import com.aieta.ticketing_app_catalog.infrastructure.database.entity.VenueJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class EventMapper {

    public EventSummary toDomain(EventJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        VenueSummary venueSummary = toVenueSummary(entity.getVenue());
        List<EventSectorSummary> sectorSummaries = entity.getSectors() != null
                ? entity.getSectors().stream().map(this::toSectorSummary).toList()
                : Collections.emptyList();

        List<String> genresList = entity.getGenres() != null
                ? Arrays.asList(entity.getGenres())
                : Collections.emptyList();

        EventStatus status;
        try {
            status = EventStatus.valueOf(entity.getStatus());
        } catch (Exception e) {
            status = EventStatus.DRAFT;
        }

        return new EventSummary(
                entity.getId(),
                entity.getSlug(),
                entity.getTitle(),
                status,
                entity.getCategory(),
                genresList,
                entity.getDoorsOpenAt(),
                entity.getStartsAt(),
                entity.getEndsAt(),
                entity.getTimezone(),
                entity.getMedia(),
                entity.getSalesPolicy(),
                venueSummary,
                sectorSummaries
        );
    }

    public VenueSummary toVenueSummary(VenueJpaEntity venue) {
        if (venue == null) {
            return null;
        }
        return new VenueSummary(
                venue.getId(),
                venue.getName(),
                venue.getCity(),
                venue.getCountry(),
                venue.getAddress(),
                venue.getLatitude(),
                venue.getLongitude(),
                venue.getSeatMapUrl(),
                venue.getImageUrl()
        );
    }

    public EventSectorSummary toSectorSummary(EventSectorJpaEntity sector) {
        if (sector == null) {
            return null;
        }
        return new EventSectorSummary(
                sector.getId(),
                sector.getName(),
                sector.getType(),
                sector.getTotalCapacity(),
                sector.getBasePrice(),
                sector.getServiceFee(),
                sector.getCurrency()
        );
    }

    public Event toDomainAggregate(EventJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        List<com.aieta.ticketing_app_catalog.domain.entity.EventSector> domainSectors = entity.getSectors() != null
                ? entity.getSectors().stream().map(this::toSectorDomain).toList()
                : Collections.emptyList();

        List<String> genresList = entity.getGenres() != null
                ? Arrays.asList(entity.getGenres())
                : Collections.emptyList();

        EventStatus status;
        try {
            status = EventStatus.valueOf(entity.getStatus());
        } catch (Exception e) {
            status = EventStatus.DRAFT;
        }

        return new com.aieta.ticketing_app_catalog.domain.entity.Event(
                entity.getId(),
                entity.getOrganizerId(),
                entity.getVenue() != null ? entity.getVenue().getId() : null,
                entity.getSlug(),
                entity.getTitle(),
                status,
                entity.getCategory(),
                genresList,
                entity.getDoorsOpenAt(),
                entity.getStartsAt(),
                entity.getEndsAt(),
                entity.getTimezone(),
                entity.getMedia(),
                entity.getSalesPolicy(),
                domainSectors,
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public com.aieta.ticketing_app_catalog.domain.entity.EventSector toSectorDomain(EventSectorJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new com.aieta.ticketing_app_catalog.domain.entity.EventSector(
                entity.getId(),
                entity.getName(),
                entity.getType(),
                entity.getTotalCapacity(),
                entity.getBasePrice(),
                entity.getServiceFee(),
                entity.getCurrency()
        );
    }

    public EventJpaEntity toJpaEntity(com.aieta.ticketing_app_catalog.domain.entity.Event domain, VenueJpaEntity venue) {
        if (domain == null) {
            return null;
        }
        EventJpaEntity entity = new EventJpaEntity();
        entity.setId(domain.getId());
        entity.setOrganizerId(domain.getOrganizerId());
        entity.setVenue(venue);
        entity.setSlug(domain.getSlug());
        entity.setTitle(domain.getTitle());
        entity.setStatus(domain.getStatus() != null ? domain.getStatus().name() : EventStatus.DRAFT.name());
        entity.setCategory(domain.getCategory());
        entity.setGenres(domain.getGenres() != null ? domain.getGenres().toArray(new String[0]) : new String[0]);
        entity.setDoorsOpenAt(domain.getDoorsOpenAt());
        entity.setStartsAt(domain.getStartsAt());
        entity.setEndsAt(domain.getEndsAt());
        entity.setTimezone(domain.getTimezone());
        entity.setMedia(domain.getMedia());
        entity.setSalesPolicy(domain.getSalesPolicy());
        entity.setVersion(domain.getVersion());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        if (domain.getSectors() != null) {
            List<EventSectorJpaEntity> sectorEntities = new java.util.ArrayList<>();
            for (var s : domain.getSectors()) {
                sectorEntities.add(toSectorJpaEntity(s, entity));
            }
            entity.setSectors(sectorEntities);
        }

        return entity;
    }

    public EventSectorJpaEntity toSectorJpaEntity(com.aieta.ticketing_app_catalog.domain.entity.EventSector sector, EventJpaEntity eventJpa) {
        if (sector == null) {
            return null;
        }
        EventSectorJpaEntity entity = new EventSectorJpaEntity();
        entity.setId(sector.getId());
        entity.setEvent(eventJpa);
        entity.setName(sector.getName());
        entity.setType(sector.getType());
        entity.setTotalCapacity(sector.getTotalCapacity());
        entity.setBasePrice(sector.getBasePrice());
        entity.setServiceFee(sector.getServiceFee());
        entity.setCurrency(sector.getCurrency());
        return entity;
    }
}
