package com.aieta.ticketing_app_catalog.infrastructure.api;

import com.aieta.ticketing_app_catalog.application.command.cancel_event.CancelEventCommand;
import com.aieta.ticketing_app_catalog.application.command.create_event.CreateEventCommand;
import com.aieta.ticketing_app_catalog.application.command.create_event.EventCreateResult;
import com.aieta.ticketing_app_catalog.application.command.publish_event.PublishEventCommand;
import com.aieta.ticketing_app_catalog.common.mediator.Mediator;
import com.aieta.ticketing_app_catalog.application.query.get_all_events.GetAllEventsQuery;
import com.aieta.ticketing_app_catalog.domain.entity.EventSummary;
import com.aieta.ticketing_app_catalog.infrastructure.api.dto.CreateEventRequest;
import com.aieta.ticketing_app_catalog.infrastructure.api.dto.EventCreatedResponse;
import com.aieta.ticketing_app_catalog.infrastructure.api.dto.EventDtoMapper;
import com.aieta.ticketing_app_catalog.infrastructure.api.dto.EventResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
public class EventAdminController {

    private final Mediator mediator;
    private final EventDtoMapper eventDtoMapper;

    public EventAdminController(Mediator mediator, EventDtoMapper eventDtoMapper) {
        this.mediator = mediator;
        this.eventDtoMapper = eventDtoMapper;
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OPERATOR')")
    @GetMapping(path = "/admin/events", version = "1.0")
    public ResponseEntity<List<EventResponse>> getAdminEvents(
            @RequestParam(name = "status", required = false, defaultValue = "ALL") String status) {
        List<EventSummary> events = mediator.dispatch(new GetAllEventsQuery(status));
        List<EventResponse> response = events.stream().map(eventDtoMapper::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OPERATOR')")
    @PostMapping(path = {"/events", "/admin/events"}, version = "1.0")
    public ResponseEntity<EventCreatedResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        List<CreateEventCommand.SectorInput> sectorInputs = request.sectors() != null
                ? request.sectors().stream()
                    .map(s -> new CreateEventCommand.SectorInput(
                            s.name(),
                            s.type(),
                            s.totalCapacity(),
                            s.basePrice(),
                            s.serviceFee(),
                            s.currency()
                    )).toList()
                : List.of();

        CreateEventCommand command = new CreateEventCommand(
                request.organizerId(),
                request.venueId(),
                request.slug(),
                request.title(),
                request.category(),
                request.genres(),
                request.doorsOpenAt(),
                request.startsAt(),
                request.endsAt(),
                request.timezone(),
                request.media(),
                request.salesPolicy(),
                sectorInputs
        );

        EventCreateResult result = mediator.dispatch(command);

        EventCreatedResponse response = new EventCreatedResponse(
                result.id(),
                result.slug(),
                result.status(),
                result.createdAt(),
                result.totalEstimatedCapacity()
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OPERATOR')")
    @PostMapping(path = {"/events/{id}/publish", "/admin/events/{id}/publish"}, version = "1.0")
    public ResponseEntity<Void> publishEvent(@PathVariable("id") String id) {
        mediator.dispatch(new PublishEventCommand(id));
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OPERATOR')")
    @DeleteMapping(path = {"/events/{id}", "/admin/events/{id}"}, version = "1.0")
    public ResponseEntity<Void> cancelEvent(
            @PathVariable("id") String id,
            @RequestParam(name = "reason", required = false, defaultValue = "Cancelled by administrator") String reason) {
        mediator.dispatch(new CancelEventCommand(id, reason));
        return ResponseEntity.ok().build();
    }
}
