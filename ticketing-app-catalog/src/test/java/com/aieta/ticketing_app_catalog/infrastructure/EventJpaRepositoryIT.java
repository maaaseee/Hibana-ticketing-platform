package com.aieta.ticketing_app_catalog.infrastructure;

import com.aieta.ticketing_app_catalog.domain.entity.EventSectorSummary;
import com.aieta.ticketing_app_catalog.domain.entity.EventStatus;
import com.aieta.ticketing_app_catalog.domain.entity.EventSummary;
import com.aieta.ticketing_app_catalog.domain.port.EventRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Integration Test: Real Database JPA Repository Adapter")
class EventJpaRepositoryIT {

    @Autowired
    private EventRepositoryPort eventRepositoryPort;

    @Test
    @DisplayName("Should retrieve upcoming published events from PostgreSQL seed data")
    void shouldFindUpcomingPublishedEvents() {
        List<EventSummary> events = eventRepositoryPort.findUpcomingPublished();

        assertNotNull(events);
        assertFalse(events.isEmpty(), "Should find at least 1 upcoming event in database");

        EventSummary coldplayEvent = events.stream()
                .filter(e -> "evt_coldplay_2026".equals(e.id()))
                .findFirst()
                .orElse(null);

        assertNotNull(coldplayEvent, "Coldplay 2026 event must be present");
        assertEquals("coldplay-music-of-the-spheres-2026", coldplayEvent.slug());
        assertEquals(EventStatus.PUBLISHED, coldplayEvent.status());
        assertEquals("CONCERT", coldplayEvent.category());
        assertNotNull(coldplayEvent.venue(), "Venue must be eagerly loaded/mapped");
        assertEquals("Estadio Nacional Julio Martínez Prádanos", coldplayEvent.venue().name());
        assertEquals("Santiago", coldplayEvent.venue().city());
        assertNotNull(coldplayEvent.venue().seatMapUrl(), "Venue should have a seatMapUrl pointing to Floci-az");

        // Verify sectors
        assertNotNull(coldplayEvent.sectors());
        assertFalse(coldplayEvent.sectors().isEmpty(), "Event should have sectors");
        assertEquals(2, coldplayEvent.sectors().size(), "Coldplay event should have Cancha VIP Front and Tribuna Andes");

        EventSectorSummary vipSector = coldplayEvent.sectors().stream()
                .filter(s -> "sec_coldplay_vip".equals(s.sectorId()))
                .findFirst()
                .orElse(null);
        assertNotNull(vipSector);
        assertEquals(new BigDecimal("180.00"), vipSector.basePrice());
        assertEquals(new BigDecimal("27.00"), vipSector.serviceFee());
        assertEquals("ARS", vipSector.currency());
        assertEquals(5000, vipSector.totalCapacity());
    }

    @Test
    @DisplayName("Should filter upcoming events by category")
    void shouldFilterUpcomingEventsByCategory() {
        List<EventSummary> concertEvents = eventRepositoryPort.findByCategoryAndPublished("CONCERT");
        assertNotNull(concertEvents);
        assertFalse(concertEvents.isEmpty());
        assertTrue(concertEvents.stream().allMatch(e -> "CONCERT".equalsIgnoreCase(e.category())));

        List<EventSummary> sportsEvents = eventRepositoryPort.findByCategoryAndPublished("SPORTS");
        assertNotNull(sportsEvents);
        assertTrue(sportsEvents.isEmpty(), "No SPORTS events exist in seed data");
    }

    @Test
    @DisplayName("Should retrieve existing event by ID")
    void shouldFindEventById() {
        Optional<EventSummary> optionalEvent = eventRepositoryPort.findById("evt_coldplay_2026");

        assertTrue(optionalEvent.isPresent());
        EventSummary event = optionalEvent.get();
        assertEquals("evt_coldplay_2026", event.id());
        assertEquals("Estadio Nacional Julio Martínez Prádanos", event.venue().name());
        assertNotNull(event.mediaJson());
        assertTrue(event.mediaJson().contains("coldplay"));
    }

    @Test
    @DisplayName("Should return empty optional for non-existent event ID")
    void shouldReturnEmptyForNonExistentId() {
        Optional<EventSummary> optionalEvent = eventRepositoryPort.findById("evt_non_existent_9999");
        assertTrue(optionalEvent.isEmpty());
    }

    @Test
    @DisplayName("Should retrieve top 3 recently added published events")
    void shouldFindRecentlyAddedPublishedEvents() {
        List<EventSummary> recent = eventRepositoryPort.findRecentlyAddedPublished();
        assertNotNull(recent);
        assertFalse(recent.isEmpty());
        assertTrue(recent.size() <= 3);
        assertTrue(recent.stream().allMatch(e -> e.status() == EventStatus.PUBLISHED));
    }

    @Test
    @DisplayName("Should retrieve top 3 upcoming published events closest to navigation date")
    void shouldFindUpcomingPublishedTop3Events() {
        List<EventSummary> upcoming = eventRepositoryPort.findUpcomingPublishedTop3(java.time.Instant.now());
        assertNotNull(upcoming);
        assertFalse(upcoming.isEmpty());
        assertTrue(upcoming.size() <= 3);
        assertTrue(upcoming.stream().allMatch(e -> e.status() == EventStatus.PUBLISHED));
    }

    @Test
    @DisplayName("Should find furthest published event date")
    void shouldFindFurthestPublishedEventDate() {
        Optional<java.time.Instant> furthestDate = eventRepositoryPort.findFurthestPublishedEventDate();
        assertNotNull(furthestDate);
        assertTrue(furthestDate.isPresent());
        assertTrue(furthestDate.get().isAfter(java.time.Instant.now()));
    }

    @Test
    @DisplayName("Should perform typo-tolerant fuzzy search matching 'Colplay' to Coldplay")
    void shouldFuzzySearchEventsWithTypos() {
        List<EventSummary> results = eventRepositoryPort.searchEventsFuzzy("Colplay", null);
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Fuzzy search with typo 'Colplay' should match Coldplay");
        assertTrue(results.stream().anyMatch(e -> "evt_coldplay_2026".equals(e.id())));
    }
}
