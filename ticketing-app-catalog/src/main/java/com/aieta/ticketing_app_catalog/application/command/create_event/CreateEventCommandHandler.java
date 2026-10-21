package com.aieta.ticketing_app_catalog.application.command.create_event;

import com.aieta.ticketing_app_catalog.common.mediator.RequestHandler;
import com.aieta.ticketing_app_catalog.domain.entity.Event;
import com.aieta.ticketing_app_catalog.domain.entity.EventSector;
import com.aieta.ticketing_app_catalog.domain.entity.EventSectorSummary;
import com.aieta.ticketing_app_catalog.domain.event.EventCreatedEvent;
import com.aieta.ticketing_app_catalog.domain.exceptions.DuplicateEventSlugException;
import com.aieta.ticketing_app_catalog.domain.exceptions.VenueNotFoundException;
import com.aieta.ticketing_app_catalog.domain.port.EventWriteRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CreateEventCommandHandler implements RequestHandler<CreateEventCommand, EventCreateResult> {

    private final EventWriteRepositoryPort repository;

    public CreateEventCommandHandler(EventWriteRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public EventCreateResult handle(CreateEventCommand command) {
        if (!repository.venueExists(command.venueId())) {
            throw new VenueNotFoundException(command.venueId());
        }

        if (repository.existsBySlug(command.slug())) {
            throw new DuplicateEventSlugException(command.slug());
        }

        String eventId = "evt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        List<EventSector> domainSectors = new ArrayList<>();
        List<EventSectorSummary> sectorSummaries = new ArrayList<>();

        if (command.sectors() != null) {
            for (var input : command.sectors()) {
                String sectorId = "sec_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                domainSectors.add(new EventSector(
                        sectorId,
                        input.name(),
                        input.type(),
                        input.totalCapacity(),
                        input.basePrice(),
                        input.serviceFee(),
                        input.currency()
                ));
                sectorSummaries.add(new EventSectorSummary(
                        sectorId,
                        input.name(),
                        input.type(),
                        input.totalCapacity(),
                        input.basePrice(),
                        input.serviceFee(),
                        input.currency()
                ));
            }
        }

        Event event = Event.createNew(
                eventId,
                command.organizerId(),
                command.venueId(),
                command.slug(),
                command.title(),
                command.category(),
                command.genres(),
                command.doorsOpenAt(),
                command.startsAt(),
                command.endsAt(),
                command.timezone(),
                command.media(),
                command.salesPolicy(),
                domainSectors
        );

        EventCreatedEvent createdEvent = new EventCreatedEvent(
                event.getId(),
                event.getOrganizerId(),
                event.getVenueId(),
                event.getSlug(),
                event.getTitle(),
                event.getStartsAt(),
                sectorSummaries,
                Instant.now()
        );

        Event saved = repository.saveWithOutbox(event, createdEvent);

        return new EventCreateResult(
                saved.getId(),
                saved.getSlug(),
                saved.getStatus(),
                saved.getCreatedAt(),
                saved.calculateTotalCapacity()
        );
    }

    @Override
    public Class<CreateEventCommand> getRequestType() {
        return CreateEventCommand.class;
    }
}
