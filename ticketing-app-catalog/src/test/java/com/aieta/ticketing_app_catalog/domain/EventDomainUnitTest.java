package com.aieta.ticketing_app_catalog.domain;

import com.aieta.ticketing_app_catalog.domain.entity.EventSectorSummary;
import com.aieta.ticketing_app_catalog.domain.entity.EventStatus;
import com.aieta.ticketing_app_catalog.domain.entity.EventSummary;
import com.aieta.ticketing_app_catalog.domain.entity.VenueSummary;
import com.aieta.ticketing_app_catalog.domain.exceptions.InvalidEventScheduleException;
import com.aieta.ticketing_app_catalog.domain.exceptions.InvalidEventTitleException;
import com.aieta.ticketing_app_catalog.domain.exceptions.InvalidSectorCapacityException;
import com.aieta.ticketing_app_catalog.domain.exceptions.InvalidSectorPricingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Test: Pure Domain Invariants with Custom Exceptions (Zero I/O)")
class EventDomainUnitTest {

    @Test
    @DisplayName("Should create EventSummary when schedule invariants hold")
    void shouldCreateEventSummaryWhenScheduleValid() {
        Instant now = Instant.now();
        Instant doors = now.plus(1, ChronoUnit.HOURS);
        Instant starts = now.plus(2, ChronoUnit.HOURS);
        Instant ends = now.plus(5, ChronoUnit.HOURS);

        VenueSummary venue = new VenueSummary("ven_1", "Test Venue", "City", "Country", "Addr", BigDecimal.ZERO, BigDecimal.ZERO);
        EventSectorSummary sector = new EventSectorSummary("sec_1", "VIP", "GA", 100, new BigDecimal("50.00"), new BigDecimal("5.00"), "USD");

        EventSummary event = new EventSummary(
                "evt_1", "test-slug", "Test Event", EventStatus.PUBLISHED,
                "CONCERT", List.of("Rock"), doors, starts, ends, "UTC",
                "{}", "{}", venue, List.of(sector)
        );

        assertNotNull(event);
        assertEquals("evt_1", event.id());
        assertEquals("Test Event", event.title());
    }

    @Test
    @DisplayName("Should throw InvalidEventScheduleException when doors open time is after start time")
    void shouldRejectWhenDoorsOpenAfterStart() {
        Instant now = Instant.now();
        Instant doors = now.plus(3, ChronoUnit.HOURS);
        Instant starts = now.plus(2, ChronoUnit.HOURS);
        Instant ends = now.plus(5, ChronoUnit.HOURS);

        assertThrows(InvalidEventScheduleException.class, () -> new EventSummary(
                "evt_1", "slug", "Title", EventStatus.DRAFT, "CONCERT",
                Collections.emptyList(), doors, starts, ends, "UTC", "{}", "{}", null, Collections.emptyList()
        ));
    }

    @Test
    @DisplayName("Should throw InvalidEventTitleException when title is null or blank")
    void shouldRejectBlankEventTitle() {
        Instant now = Instant.now();
        Instant doors = now.plus(1, ChronoUnit.HOURS);
        Instant starts = now.plus(2, ChronoUnit.HOURS);
        Instant ends = now.plus(5, ChronoUnit.HOURS);

        assertThrows(InvalidEventTitleException.class, () -> new EventSummary(
                "evt_1", "slug", "   ", EventStatus.DRAFT, "CONCERT",
                Collections.emptyList(), doors, starts, ends, "UTC", "{}", "{}", null, Collections.emptyList()
        ));
    }

    @Test
    @DisplayName("Should throw InvalidSectorCapacityException when sector capacity is zero or negative")
    void shouldRejectInvalidSectorCapacity() {
        assertThrows(InvalidSectorCapacityException.class, () -> new EventSectorSummary(
                "sec_1", "VIP", "GA", 0, new BigDecimal("50.00"), BigDecimal.ZERO, "USD"
        ));
    }

    @Test
    @DisplayName("Should throw InvalidSectorPricingException when base price is non-positive")
    void shouldRejectInvalidSectorPricing() {
        assertThrows(InvalidSectorPricingException.class, () -> new EventSectorSummary(
                "sec_1", "VIP", "GA", 100, BigDecimal.ZERO, BigDecimal.ZERO, "USD"
        ));
    }
}
