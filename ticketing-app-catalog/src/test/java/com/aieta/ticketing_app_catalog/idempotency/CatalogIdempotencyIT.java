package com.aieta.ticketing_app_catalog.idempotency;

import com.aieta.ticketing_app_catalog.application.query.get_event_by_id.GetEventByIdQuery;
import com.aieta.ticketing_app_catalog.application.query.get_upcoming_events.GetUpcomingEventsQuery;
import com.aieta.ticketing_app_catalog.common.mediator.Mediator;
import com.aieta.ticketing_app_catalog.domain.entity.EventSummary;
import com.aieta.ticketing_app_catalog.domain.exceptions.EventNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Idempotency Test: Resilience Against Retries via Mediator (Milestone 5e)")
class CatalogIdempotencyIT {

    @Autowired
    private Mediator mediator;

    @Test
    @DisplayName("Should guarantee deterministic, invariant output across 30 repeated retries")
    void shouldProduceIdempotentResultsAcrossRetries() {
        final int retryCount = 30;
        final String eventId = "evt_coldplay_2026";

        EventSummary baseline = mediator.dispatch(new GetEventByIdQuery(eventId));
        assertNotNull(baseline);

        for (int attempt = 1; attempt <= retryCount; attempt++) {
            EventSummary current = mediator.dispatch(new GetEventByIdQuery(eventId));

            assertEquals(baseline.id(), current.id(), "ID mismatch at attempt " + attempt);
            assertEquals(baseline.slug(), current.slug(), "Slug mismatch at attempt " + attempt);
            assertEquals(baseline.title(), current.title(), "Title mismatch at attempt " + attempt);
            assertEquals(baseline.status(), current.status(), "Status mismatch at attempt " + attempt);
            assertEquals(baseline.startsAt(), current.startsAt(), "Start time mismatch at attempt " + attempt);
            assertEquals(baseline.venue().id(), current.venue().id(), "Venue mismatch at attempt " + attempt);
            assertEquals(baseline.sectors().size(), current.sectors().size(), "Sectors count mismatch at attempt " + attempt);
            assertEquals(baseline.sectors().getFirst().basePrice(), current.sectors().getFirst().basePrice());
        }
    }

    @Test
    @DisplayName("Should consistently produce idempotent 404 Not Found exceptions across retries")
    void shouldProduceIdempotentFailuresAcrossRetries() {
        final int retryCount = 20;
        final String nonExistentId = "evt_phantom_retry_999";

        for (int attempt = 1; attempt <= retryCount; attempt++) {
            assertThrows(EventNotFoundException.class, () ->
                    mediator.dispatch(new GetEventByIdQuery(nonExistentId))
            );
        }
    }

    @Test
    @DisplayName("Should return consistent result set across repeated list queries with category filter")
    void shouldProduceIdempotentListQueries() {
        final int retryCount = 20;
        List<EventSummary> baselineList = mediator.dispatch(new GetUpcomingEventsQuery("CONCERT"));
        assertFalse(baselineList.isEmpty());

        for (int attempt = 1; attempt <= retryCount; attempt++) {
            List<EventSummary> currentList = mediator.dispatch(new GetUpcomingEventsQuery("CONCERT"));
            assertEquals(baselineList.size(), currentList.size(), "List size changed at attempt " + attempt);
            assertEquals(baselineList.getFirst().id(), currentList.getFirst().id());
        }
    }
}
