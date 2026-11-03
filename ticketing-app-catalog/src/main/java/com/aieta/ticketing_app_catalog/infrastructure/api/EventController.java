package com.aieta.ticketing_app_catalog.infrastructure.api;

import com.aieta.ticketing_app_catalog.application.query.get_all_events.GetAllEventsQuery;
import com.aieta.ticketing_app_catalog.application.query.get_catalog_metadata.GetCatalogMetadataQuery;
import com.aieta.ticketing_app_catalog.application.query.get_event_by_id.GetEventByIdQuery;
import com.aieta.ticketing_app_catalog.application.query.get_home_highlights.GetHomeHighlightsQuery;
import com.aieta.ticketing_app_catalog.application.query.get_upcoming_events.GetUpcomingEventsQuery;
import com.aieta.ticketing_app_catalog.application.query.search_events.SearchEventsQuery;
import com.aieta.ticketing_app_catalog.common.mediator.Mediator;
import com.aieta.ticketing_app_catalog.domain.entity.CatalogMetadata;
import com.aieta.ticketing_app_catalog.domain.entity.EventSectorSummary;
import com.aieta.ticketing_app_catalog.domain.entity.EventSummary;
import com.aieta.ticketing_app_catalog.domain.entity.HomeHighlights;
import com.aieta.ticketing_app_catalog.domain.entity.VenueSummary;
import com.aieta.ticketing_app_catalog.infrastructure.api.dto.CatalogMetadataResponse;
import com.aieta.ticketing_app_catalog.infrastructure.api.dto.EventResponse;
import com.aieta.ticketing_app_catalog.infrastructure.api.dto.HomeHighlightsResponse;
import com.aieta.ticketing_app_catalog.infrastructure.api.dto.SectorResponse;
import com.aieta.ticketing_app_catalog.infrastructure.api.dto.VenueResponse;
import com.aieta.ticketing_app_catalog.infrastructure.cache.CatalogCacheService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {

    private final Mediator mediator;
    private final CatalogCacheService cacheService;
    private final com.aieta.ticketing_app_catalog.infrastructure.api.dto.EventDtoMapper eventDtoMapper;

    public EventController(
            Mediator mediator,
            CatalogCacheService cacheService,
            com.aieta.ticketing_app_catalog.infrastructure.api.dto.EventDtoMapper eventDtoMapper) {
        this.mediator = mediator;
        this.cacheService = cacheService;
        this.eventDtoMapper = eventDtoMapper;
    }

    @GetMapping(version = "1.0")
    public ResponseEntity<List<EventResponse>> getUpcomingEvents(
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "venue", required = false) String venue,
            @RequestParam(name = "status", required = false) String status) {
        // Enforce public restriction: non-public status queries (ALL, DRAFT, CANCELLED) require /admin/events (SEC-04)
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("PUBLISHED")) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Querying non-public event states requires administrative access via GET /api/v1/admin/events"
            );
        }

        List<EventSummary> events;
        if ((query != null && !query.isBlank()) || (venue != null && !venue.isBlank())) {
            events = mediator.dispatch(new SearchEventsQuery(query, venue));
        } else {
            events = mediator.dispatch(new GetUpcomingEventsQuery(category));
        }
        List<EventResponse> response = events.stream().map(eventDtoMapper::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping(path = "/highlights", version = "1.0")
    public ResponseEntity<HomeHighlightsResponse> getHomeHighlights(
            @RequestParam(name = "referenceDate", required = false) Instant referenceDate) {
        String cacheKey = referenceDate != null
                ? "catalog:highlights:" + referenceDate.getEpochSecond() / 3600
                : "catalog:highlights:latest";

        HomeHighlightsResponse response = cacheService.getOrFetch(
                cacheKey,
                Duration.ofMinutes(10),
                HomeHighlightsResponse.class,
                () -> {
                    HomeHighlights highlights = mediator.dispatch(new GetHomeHighlightsQuery(referenceDate));
                    List<EventResponse> recentResponses = highlights.recentlyAdded().stream().map(eventDtoMapper::toResponse).toList();
                    List<EventResponse> upcomingResponses = highlights.upcoming().stream().map(eventDtoMapper::toResponse).toList();
                    return new HomeHighlightsResponse(recentResponses, upcomingResponses);
                }
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=300, stale-while-revalidate=600")
                .body(response);
    }

    @GetMapping(path = "/meta", version = "1.0")
    public ResponseEntity<CatalogMetadataResponse> getCatalogMetadata() {
        CatalogMetadataResponse response = cacheService.getOrFetch(
                "catalog:metadata",
                Duration.ofMinutes(10),
                CatalogMetadataResponse.class,
                () -> {
                    CatalogMetadata meta = mediator.dispatch(new GetCatalogMetadataQuery());
                    List<VenueResponse> venueResponses = meta.venues().stream().map(eventDtoMapper::mapVenueResponse).toList();
                    return new CatalogMetadataResponse(meta.furthestEventDate(), venueResponses);
                }
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=300, stale-while-revalidate=600")
                .body(response);
    }

    @GetMapping(path = "/search", version = "1.0")
    public ResponseEntity<List<EventResponse>> searchEvents(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "venue", required = false) String venue) {
        List<EventSummary> events = mediator.dispatch(new SearchEventsQuery(query, venue));
        List<EventResponse> response = events.stream().map(eventDtoMapper::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping(path = "/{id}", version = "1.0")
    public ResponseEntity<EventResponse> getEventById(@PathVariable("id") String id) {
        EventSummary event = mediator.dispatch(new GetEventByIdQuery(id));
        return ResponseEntity.ok(eventDtoMapper.toResponse(event));
    }
}
