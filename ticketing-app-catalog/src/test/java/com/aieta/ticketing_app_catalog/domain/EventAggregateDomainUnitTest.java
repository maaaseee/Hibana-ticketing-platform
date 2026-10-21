package com.aieta.ticketing_app_catalog.domain;

import com.aieta.ticketing_app_catalog.domain.entity.Event;
import com.aieta.ticketing_app_catalog.domain.entity.EventSector;
import com.aieta.ticketing_app_catalog.domain.entity.EventStatus;
import com.aieta.ticketing_app_catalog.domain.exceptions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Test: Pure Event Domain Aggregate Invariants (Zero I/O)")
class EventAggregateDomainUnitTest {

    private final Instant now = Instant.now();
    private final Instant doorsOpen = now.plus(1, ChronoUnit.DAYS);
    private final Instant startsAt = doorsOpen.plus(2, ChronoUnit.HOURS);
    private final Instant endsAt = startsAt.plus(3, ChronoUnit.HOURS);

    private EventSector createValidSector(String name, int capacity) {
        return new EventSector(
                "sec_01",
                name,
                "GENERAL_ADMISSION",
                capacity,
                new BigDecimal("50.00"),
                new BigDecimal("5.00"),
                "USD"
        );
    }

    @Test
    @DisplayName("Should instantiate valid Event aggregate in DRAFT status")
    void shouldCreateValidDraftEvent() {
        EventSector sector = createValidSector("Platea VIP", 500);
        Event event = Event.createNew(
                "evt_test_01",
                "org_01",
                "ven_01",
                "rock-fest-2026",
                "Rock Fest 2026",
                "CONCERT",
                List.of("ROCK", "INDIE"),
                doorsOpen,
                startsAt,
                endsAt,
                "America/Santiago",
                "{}",
                "{}",
                List.of(sector)
        );

        assertNotNull(event);
        assertEquals("evt_test_01", event.getId());
        assertEquals(EventStatus.DRAFT, event.getStatus());
        assertEquals("rock-fest-2026", event.getSlug());
        assertEquals(500, event.calculateTotalCapacity());
        assertEquals(1, event.getSectors().size());
    }

    @Test
    @DisplayName("Should reject blank title with InvalidEventTitleException")
    void shouldRejectBlankTitle() {
        EventSector sector = createValidSector("Platea", 100);
        assertThrows(InvalidEventTitleException.class, () ->
                Event.createNew(
                        "evt_01", "org_01", "ven_01", "slug-01", "   ",
                        "CONCERT", List.of(), doorsOpen, startsAt, endsAt, "UTC", "{}", "{}", List.of(sector)
                )
        );
    }

    @Test
    @DisplayName("Should reject blank identifier with InvalidEventIdentifierException")
    void shouldRejectBlankIdentifier() {
        EventSector sector = createValidSector("Platea", 100);
        assertThrows(InvalidEventIdentifierException.class, () ->
                Event.createNew(
                        "  ", "org_01", "ven_01", "slug-01", "Valid Title",
                        "CONCERT", List.of(), doorsOpen, startsAt, endsAt, "UTC", "{}", "{}", List.of(sector)
                )
        );
    }

    @Test
    @DisplayName("Should reject doors open time after starts at time")
    void shouldRejectDoorsOpenAfterStartsAt() {
        EventSector sector = createValidSector("Platea", 100);
        Instant invertedDoors = startsAt.plus(1, ChronoUnit.HOURS);

        assertThrows(InvalidEventScheduleException.class, () ->
                Event.createNew(
                        "evt_01", "org_01", "ven_01", "slug-01", "Valid Title",
                        "CONCERT", List.of(), invertedDoors, startsAt, endsAt, "UTC", "{}", "{}", List.of(sector)
                )
        );
    }

    @Test
    @DisplayName("Should reject starts at time after ends at time")
    void shouldRejectStartsAtAfterEndsAt() {
        EventSector sector = createValidSector("Platea", 100);
        Instant invertedEnds = startsAt.minus(1, ChronoUnit.HOURS);

        assertThrows(InvalidEventScheduleException.class, () ->
                Event.createNew(
                        "evt_01", "org_01", "ven_01", "slug-01", "Valid Title",
                        "CONCERT", List.of(), doorsOpen, startsAt, invertedEnds, "UTC", "{}", "{}", List.of(sector)
                )
        );
    }

    @Test
    @DisplayName("Should transition from DRAFT to PUBLISHED when sectors are present")
    void shouldPublishDraftEventWithSectors() {
        EventSector sector = createValidSector("General", 200);
        Event event = Event.createNew(
                "evt_01", "org_01", "ven_01", "slug-01", "Valid Title",
                "CONCERT", List.of(), doorsOpen, startsAt, endsAt, "UTC", "{}", "{}", List.of(sector)
        );

        event.publish();
        assertEquals(EventStatus.PUBLISHED, event.getStatus());
    }

    @Test
    @DisplayName("Should throw InvalidEventStateException when publishing event with zero sectors")
    void shouldThrowWhenPublishingWithoutSectors() {
        Event event = Event.createNew(
                "evt_01", "org_01", "ven_01", "slug-01", "Valid Title",
                "CONCERT", List.of(), doorsOpen, startsAt, endsAt, "UTC", "{}", "{}", Collections.emptyList()
        );

        assertThrows(InvalidEventStateException.class, event::publish);
    }

    @Test
    @DisplayName("Should transition to CANCELLED and reject further state mutations")
    void shouldCancelEventAndRejectMutations() {
        EventSector sector = createValidSector("General", 200);
        Event event = Event.createNew(
                "evt_01", "org_01", "ven_01", "slug-01", "Valid Title",
                "CONCERT", List.of(), doorsOpen, startsAt, endsAt, "UTC", "{}", "{}", List.of(sector)
        );

        event.cancel("Artist illness");
        assertEquals(EventStatus.CANCELLED, event.getStatus());

        assertThrows(EventAlreadyCancelledException.class, () -> event.cancel("Duplicate cancel"));
        assertThrows(EventAlreadyCancelledException.class, event::publish);
    }

    @Test
    @DisplayName("Should reject sector with zero or negative capacity")
    void shouldRejectInvalidSectorCapacity() {
        assertThrows(InvalidSectorCapacityException.class, () ->
                new EventSector("sec_01", "Sector", "GENERAL", 0, new BigDecimal("10.00"), BigDecimal.ZERO, "USD")
        );
        assertThrows(InvalidSectorCapacityException.class, () ->
                new EventSector("sec_01", "Sector", "GENERAL", -5, new BigDecimal("10.00"), BigDecimal.ZERO, "USD")
        );
    }

    @Test
    @DisplayName("Should reject sector with negative price or fee")
    void shouldRejectInvalidSectorPricing() {
        assertThrows(InvalidSectorPricingException.class, () ->
                new EventSector("sec_01", "Sector", "GENERAL", 100, new BigDecimal("-1.00"), BigDecimal.ZERO, "USD")
        );
        assertThrows(InvalidSectorPricingException.class, () ->
                new EventSector("sec_01", "Sector", "GENERAL", 100, new BigDecimal("10.00"), new BigDecimal("-0.01"), "USD")
        );
    }
}
