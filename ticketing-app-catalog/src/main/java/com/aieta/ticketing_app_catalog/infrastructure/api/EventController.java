package com.aieta.ticketing_app_catalog.infrastructure.api;

import com.aieta.ticketing_app_catalog.application.query.get_event_by_id.GetEventByIdQuery;
import com.aieta.ticketing_app_catalog.application.query.get_upcoming_events.GetUpcomingEventsQuery;
import com.aieta.ticketing_app_catalog.common.mediator.Mediator;
import com.aieta.ticketing_app_catalog.domain.entity.EventSectorSummary;
import com.aieta.ticketing_app_catalog.domain.entity.EventSummary;
import com.aieta.ticketing_app_catalog.infrastructure.api.dto.EventResponse;
import com.aieta.ticketing_app_catalog.infrastructure.api.dto.SectorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {

    private final Mediator mediator;
    private final ObjectMapper objectMapper;

    public EventController(Mediator mediator, ObjectMapper objectMapper) {
        this.mediator = mediator;
        this.objectMapper = objectMapper;
    }

    @GetMapping(version = "1.0")
    public ResponseEntity<List<EventResponse>> getUpcomingEvents(@RequestParam(name = "category", required = false) String category) {
        List<EventSummary> events = mediator.dispatch(new GetUpcomingEventsQuery(category));
        List<EventResponse> response = events.stream().map(this::mapToResponse).toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping(path = "/{id}", version = "1.0")
    public ResponseEntity<EventResponse> getEventById(@PathVariable("id") String id) {
        EventSummary event = mediator.dispatch(new GetEventByIdQuery(id));
        return ResponseEntity.ok(mapToResponse(event));
    }

    private EventResponse mapToResponse(EventSummary event) {
        List<SectorResponse> sectorResponses = event.sectors() != null
                ? event.sectors().stream().map(this::mapSectorResponse).toList()
                : Collections.emptyList();

        BigDecimal minPrice = sectorResponses.stream()
            .map(sector -> sector.totalPrice())
                .min((left, right) -> left.compareTo(right))
                .orElse(BigDecimal.ZERO);

        String currency = sectorResponses.isEmpty() ? "USD" : sectorResponses.getFirst().currency();

        String bannerUrl = extractJsonField(event.mediaJson(), "bannerUrl");
        String posterUrl = extractJsonField(event.mediaJson(), "posterUrl");

        return new EventResponse(
                event.id(),
                event.slug(),
                event.title(),
                event.status().name(),
                event.category(),
                event.genres(),
                event.doorsOpenAt(),
                event.startsAt(),
                event.endsAt(),
                event.timezone(),
                event.venue() != null ? event.venue().name() : null,
                event.venue() != null ? event.venue().city() : null,
                event.venue() != null ? event.venue().address() : null,
                event.venue() != null ? event.venue().seatMapUrl() : null,
                minPrice,
                currency,
                bannerUrl,
                posterUrl,
                sectorResponses
        );
    }

    private SectorResponse mapSectorResponse(EventSectorSummary sector) {
        BigDecimal total = sector.basePrice().add(sector.serviceFee());
        return new SectorResponse(
                sector.sectorId(),
                sector.name(),
                sector.type(),
                sector.totalCapacity(),
                sector.basePrice(),
                sector.serviceFee(),
                total,
                sector.currency()
        );
    }

    private String extractJsonField(String jsonString, String fieldName) {
        if (jsonString == null || jsonString.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(jsonString);
            JsonNode field = node.get(fieldName);
            return field != null ? field.asString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
