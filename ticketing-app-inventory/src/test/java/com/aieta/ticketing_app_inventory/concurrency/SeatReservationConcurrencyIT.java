package com.aieta.ticketing_app_inventory.concurrency;

import com.aieta.ticketing_app_inventory.application.commands.hold_seat.HoldSeatCommand;
import com.aieta.ticketing_app_inventory.application.commands.hold_seat.HoldSeatCommandHandler;
import com.aieta.ticketing_app_inventory.application.commands.hold_seat.ReservationResult;
import com.aieta.ticketing_app_inventory.application.common.mediator.Mediator;
import com.aieta.ticketing_app_inventory.domain.enums.SeatStatus;
import com.aieta.ticketing_app_inventory.domain.exceptions.SeatAlreadyHeldException;
import com.aieta.ticketing_app_inventory.domain.model.Money;
import com.aieta.ticketing_app_inventory.domain.model.Seat;
import com.aieta.ticketing_app_inventory.domain.model.SeatId;
import com.aieta.ticketing_app_inventory.domain.ports.DistributedLockPort;
import com.aieta.ticketing_app_inventory.domain.ports.ReservationRepositoryPort;
import com.aieta.ticketing_app_inventory.domain.ports.SeatRepositoryPort;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.adapter.JpaReservationRepositoryAdapter;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.adapter.JpaSeatRepositoryAdapter;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.repository.SpringDataOutboxRepository;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.repository.SpringDataReservationRepository;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.repository.SpringDataSeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Concurrency Race Condition: 500 Virtual Threads Colliding on Single Seat")
class SeatReservationConcurrencyIT {

    @Autowired
    private SpringDataSeatRepository seatRepository;

    @Autowired
    private SpringDataReservationRepository reservationRepository;

    @Autowired
    private SpringDataOutboxRepository outboxRepository;

    @Autowired
    private JpaSeatRepositoryAdapter seatRepositoryAdapter;

    @Autowired
    private JpaReservationRepositoryAdapter reservationRepositoryAdapter;

    private static class AtomicConcurrentLockAdapter implements DistributedLockPort {
        private final ConcurrentHashMap<String, String> locks = new ConcurrentHashMap<>();

        @Override
        public boolean acquireSeatLock(String eventId, String seatId, String userId, Duration ttl) {
            String key = "lock:event:" + eventId + ":seat:" + seatId;
            return locks.putIfAbsent(key, userId) == null;
        }

        @Override
        public boolean releaseSeatLock(String eventId, String seatId, String userId) {
            String key = "lock:event:" + eventId + ":seat:" + seatId;
            return locks.remove(key, userId);
        }

        @Override
        public void forceReleaseSeatLock(String eventId, String seatId) {
            locks.remove("lock:event:" + eventId + ":seat:" + seatId);
        }

        public boolean hasLock(String eventId, String seatId) {
            return locks.containsKey("lock:event:" + eventId + ":seat:" + seatId);
        }
    }

    private AtomicConcurrentLockAdapter lockAdapter;
    private HoldSeatCommandHandler commandHandler;
    private Mediator mediator;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        outboxRepository.deleteAll();
        seatRepository.deleteAll();

        lockAdapter = new AtomicConcurrentLockAdapter();
        commandHandler = new HoldSeatCommandHandler(lockAdapter, seatRepositoryAdapter, reservationRepositoryAdapter);
        mediator = new Mediator(List.of(commandHandler));
    }

    @Test
    @DisplayName("500 virtual threads simultaneously holding seat 104: exactly 1 succeeds, 499 get SeatAlreadyHeldException")
    void shouldHandle500ConcurrentVirtualThreadsZeroDoubleBooking() throws InterruptedException {
        final int concurrencyLevel = 500;
        final String eventId = "evt_coldplay_2026";
        final String seatId = "seat_coldplay_104";

        // Setup seat in AVAILABLE status in database
        Seat seat = Seat.createNew(
                SeatId.of(seatId),
                eventId,
                "sec_vip",
                "Row-1",
                "104",
                Money.of(new BigDecimal("180.00"), "USD")
        );
        seatRepositoryAdapter.save(seat);

        CountDownLatch readyLatch = new CountDownLatch(concurrencyLevel);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(concurrencyLevel);

        List<ReservationResult> successes = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> rejections = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger conflictCount = new AtomicInteger(0);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < concurrencyLevel; i++) {
                final String userId = "user_" + UUID.randomUUID().toString().substring(0, 8);
                executor.submit(() -> {
                    readyLatch.countDown();
                    try {
                        startLatch.await();
                        HoldSeatCommand command = new HoldSeatCommand(eventId, seatId, userId);
                        ReservationResult result = mediator.dispatch(command);
                        successes.add(result);
                    } catch (Throwable t) {
                        rejections.add(t);
                        Throwable current = t;
                        boolean isConflict = false;
                        while (current != null) {
                            if (current instanceof SeatAlreadyHeldException) {
                                isConflict = true;
                                break;
                            }
                            current = current.getCause();
                        }
                        if (isConflict) {
                            conflictCount.incrementAndGet();
                        }
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            // Wait for all 500 virtual threads to be initialized and waiting at start line
            assertTrue(readyLatch.await(15, TimeUnit.SECONDS), "All 500 virtual threads must be ready");

            // Release starter latch
            startLatch.countDown();

            // Wait for all 500 threads to complete
            assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "All 500 threads must complete execution within 30s");

            // Invariant 1: Exactly 1 thread successfully acquired the seat
            assertEquals(1, successes.size(), "Strictly 1 virtual thread must acquire the reservation");

            // Invariant 2: Exactly 499 threads were rejected
            assertEquals(concurrencyLevel - 1, rejections.size(), "Remaining 499 threads must be rejected");

            // Invariant 3: All 499 rejections are SeatAlreadyHeldException (HTTP 409 Conflict)
            assertEquals(concurrencyLevel - 1, conflictCount.get(), "All 499 rejected threads must encounter SeatAlreadyHeldException");

            // Invariant 4: Zero double-booking in database
            assertEquals(1, reservationRepository.countBySeatId(seatId), "Database must contain exactly 1 reservation record for this seat");

            // Invariant 5: Lock is active
            assertTrue(lockAdapter.hasLock(eventId, seatId), "Seat lock must be held");

            // Invariant 6: Seat in database transitioned to HELD
            Seat persistedSeat = seatRepositoryAdapter.findByEventIdAndSeatId(eventId, seatId).orElseThrow();
            assertEquals(SeatStatus.HELD, persistedSeat.getStatus(), "Seat status in DB must be HELD");

            // Invariant 7: Outbox event was created for the winning reservation
            assertEquals(1, outboxRepository.count(), "Exactly 1 Outbox event must be persisted for the winning reservation");
        }
    }
}
