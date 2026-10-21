package com.aieta.ticketing_app_catalog.concurrent;

import com.aieta.ticketing_app_catalog.application.command.create_event.CreateEventCommand;
import com.aieta.ticketing_app_catalog.application.command.create_event.EventCreateResult;
import com.aieta.ticketing_app_catalog.common.mediator.Mediator;
import com.aieta.ticketing_app_catalog.domain.exceptions.DuplicateEventSlugException;
import com.aieta.ticketing_app_catalog.infrastructure.database.repository.SpringDataEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Concurrency Test: Simultaneous Event Creation Collision (Milestone 5.4)")
class EventCreationConcurrencyIT {

    @Autowired
    private Mediator mediator;

    @Autowired
    private SpringDataEventRepository eventRepository;

    @Test
    @DisplayName("Should handle 50 concurrent virtual threads attempting to create the same event slug: exactly 1 succeeds, 49 fail with DuplicateEventSlugException")
    void shouldHandleConcurrentSlugCollisionsDeterministically() throws InterruptedException {
        final int concurrencyLevel = 50;
        final String sharedSlug = "concurrent-rock-fest-" + UUID.randomUUID().toString().substring(0, 8);
        Instant now = Instant.now().plus(60, ChronoUnit.DAYS);

        CountDownLatch readyLatch = new CountDownLatch(concurrencyLevel);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(concurrencyLevel);

        List<EventCreateResult> successfulCreations = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> caughtExceptions = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger duplicateCount = new AtomicInteger(0);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < concurrencyLevel; i++) {
                final int index = i;
                executor.submit(() -> {
                    readyLatch.countDown();
                    try {
                        startLatch.await();
                        CreateEventCommand command = new CreateEventCommand(
                                "org_amaryllis_live",
                                "ven_estadio_nacional",
                                sharedSlug,
                                "Concurrent Rock Fest " + index,
                                "CONCERT",
                                List.of("Rock"),
                                now,
                                now.plus(2, ChronoUnit.HOURS),
                                now.plus(5, ChronoUnit.HOURS),
                                "UTC",
                                "{}",
                                "{}",
                                List.of(new CreateEventCommand.SectorInput(
                                        "Platea", "GENERAL_ADMISSION", 100, BigDecimal.valueOf(50), BigDecimal.valueOf(5), "USD"
                                ))
                        );
                        EventCreateResult result = mediator.dispatch(command);
                        successfulCreations.add(result);
                    } catch (Throwable t) {
                        caughtExceptions.add(t);
                        Throwable root = t;
                        boolean isDuplicate = false;
                        while (root != null) {
                            if (root instanceof DuplicateEventSlugException) {
                                isDuplicate = true;
                                break;
                            }
                            root = root.getCause();
                        }
                        if (isDuplicate) {
                            duplicateCount.incrementAndGet();
                        }
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            assertTrue(readyLatch.await(5, TimeUnit.SECONDS), "All 50 threads should be ready within 5s");

            long startTime = System.currentTimeMillis();
            startLatch.countDown();

            assertTrue(doneLatch.await(15, TimeUnit.SECONDS), "All 50 creations should finish within 15s");
            long duration = System.currentTimeMillis() - startTime;

            assertEquals(1, successfulCreations.size(), "Exactly 1 concurrent request must succeed");
            assertEquals(concurrencyLevel - 1, caughtExceptions.size(), "Exactly 49 requests must fail");
            assertEquals(concurrencyLevel - 1, duplicateCount.get(), "All 49 failures must be DuplicateEventSlugException");

            var found = eventRepository.findBySlug(sharedSlug);
            assertTrue(found.isPresent(), "Event must be saved in database");
            assertEquals(sharedSlug, found.get().getSlug());

            System.out.printf("Processed %d concurrent virtual threads colliding on slug '%s' in %d ms: 1 WINNER, %d REJECTIONS%n",
                    concurrencyLevel, sharedSlug, duration, duplicateCount.get());
        }
    }
}
