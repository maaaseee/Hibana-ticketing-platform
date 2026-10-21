package com.aieta.ticketing_app_catalog.concurrent;

import com.aieta.ticketing_app_catalog.application.query.get_event_by_id.GetEventByIdQuery;
import com.aieta.ticketing_app_catalog.common.mediator.Mediator;
import com.aieta.ticketing_app_catalog.domain.entity.EventSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Concurrency Test: Parallel Requests On Same Record via Mediator (Milestone 5d)")
class CatalogConcurrencyIT {

    @Autowired
    private Mediator mediator;

    @Test
    @DisplayName("Should handle 50 concurrent Virtual Threads querying the same event via Mediator without race conditions")
    void shouldHandleConcurrentQueriesOnSameRegistry() throws InterruptedException {
        final int concurrencyLevel = 50;
        final String eventId = "evt_coldplay_2026";

        CountDownLatch readyLatch = new CountDownLatch(concurrencyLevel);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(concurrencyLevel);

        List<EventSummary> results = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger successCounter = new AtomicInteger(0);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < concurrencyLevel; i++) {
                executor.submit(() -> {
                    readyLatch.countDown();
                    try {
                        startLatch.await(); // wait for simultaneous start trigger
                        EventSummary summary = mediator.dispatch(new GetEventByIdQuery(eventId));
                        results.add(summary);
                        successCounter.incrementAndGet();
                    } catch (Throwable t) {
                        errors.add(t);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            // Wait for all virtual threads to be queued and ready
            assertTrue(readyLatch.await(5, TimeUnit.SECONDS), "All threads should be ready within 5s");

            // Unleash all threads simultaneously
            long startTime = System.currentTimeMillis();
            startLatch.countDown();

            // Wait for all to finish
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "All concurrent requests should complete within 10s");
            long duration = System.currentTimeMillis() - startTime;

            // Assertions
            assertTrue(errors.isEmpty(), "Zero exceptions expected under concurrency: " + errors);
            assertEquals(concurrencyLevel, successCounter.get(), "All 50 requests should succeed");
            assertEquals(concurrencyLevel, results.size(), "Result size must equal concurrency level");

            // Invariant verification across all concurrent results
            for (EventSummary event : results) {
                assertEquals(eventId, event.id());
                assertEquals("coldplay-music-of-the-spheres-2026", event.slug());
                assertEquals("Estadio Nacional Julio Martínez Prádanos", event.venue().name());
                assertEquals(2, event.sectors().size());
            }

            System.out.printf("Processed %d concurrent virtual thread queries on same record via Mediator in %d ms%n",
                    concurrencyLevel, duration);
        }
    }
}
