package com.aieta.ticketing_app_catalog.infrastructure.api;

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
    private final ObjectMapper objectMapper;
    private final CatalogCacheService cacheService;

    public EventController(Mediator mediator, ObjectMapper objectMapper, CatalogCacheService cacheService) {
        this.mediator = mediator;
        this.objectMapper = objectMapper;
        this.cacheService = cacheService;
    }

    @GetMapping(version = "1.0")
    public ResponseEntity<List<EventResponse>> getUpcomingEvents(
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "venue", required = false) String venue) {
        List<EventSummary> events;
        if ((query != null && !query.isBlank()) || (venue != null && !venue.isBlank())) {
            events = mediator.dispatch(new SearchEventsQuery(query, venue));
        } else {
            events = mediator.dispatch(new GetUpcomingEventsQuery(category));
        }
        List<EventResponse> response = events.stream().map(this::mapToResponse).toList();
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
                    List<EventResponse> recentResponses = highlights.recentlyAdded().stream().map(this::mapToResponse).toList();
                    List<EventResponse> upcomingResponses = highlights.upcoming().stream().map(this::mapToResponse).toList();
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
                    List<VenueResponse> venueResponses = meta.venues().stream().map(this::mapVenueResponse).toList();
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

        String currency = sectorResponses.isEmpty() ? "ARS" : sectorResponses.getFirst().currency();

        String bannerUrl = extractJsonField(event.mediaJson(), "bannerUrl");
        String posterUrl = extractJsonField(event.mediaJson(), "posterUrl");
        String description = extractJsonField(event.mediaJson(), "description");

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
                event.venue() != null ? event.venue().imageUrl() : null,
                minPrice,
                currency,
                bannerUrl,
                posterUrl,
                description,
                event.salesPolicyJson(),
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

    private VenueResponse mapVenueResponse(VenueSummary venue) {
        return new VenueResponse(
                venue.id(),
                venue.name(),
                venue.city(),
                venue.country(),
                venue.address(),
                venue.latitude(),
                venue.longitude(),
                venue.seatMapUrl(),
                venue.imageUrl()
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
