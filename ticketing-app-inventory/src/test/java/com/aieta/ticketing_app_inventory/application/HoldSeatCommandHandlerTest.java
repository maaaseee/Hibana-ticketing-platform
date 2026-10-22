package com.aieta.ticketing_app_inventory.application;

import com.aieta.ticketing_app_inventory.application.commands.hold_seat.HoldSeatCommand;
import com.aieta.ticketing_app_inventory.application.commands.hold_seat.HoldSeatCommandHandler;
import com.aieta.ticketing_app_inventory.application.commands.hold_seat.ReservationResult;
import com.aieta.ticketing_app_inventory.domain.enums.ReservationStatus;
import com.aieta.ticketing_app_inventory.domain.enums.SeatStatus;
import com.aieta.ticketing_app_inventory.domain.exceptions.SeatAlreadyHeldException;
import com.aieta.ticketing_app_inventory.domain.exceptions.SeatNotFoundException;
import com.aieta.ticketing_app_inventory.domain.model.Money;
import com.aieta.ticketing_app_inventory.domain.model.Seat;
import com.aieta.ticketing_app_inventory.domain.model.SeatId;
import com.aieta.ticketing_app_inventory.domain.model.TicketReservation;
import com.aieta.ticketing_app_inventory.domain.ports.DistributedLockPort;
import com.aieta.ticketing_app_inventory.domain.ports.ReservationRepositoryPort;
import com.aieta.ticketing_app_inventory.domain.ports.SeatRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("Unit Test: HoldSeatCommandHandler with Mock Ports")
class HoldSeatCommandHandlerTest {

    private DistributedLockPort distributedLockPort;
    private SeatRepositoryPort seatRepositoryPort;
    private ReservationRepositoryPort reservationRepositoryPort;
    private HoldSeatCommandHandler handler;

    @BeforeEach
    void setUp() {
        distributedLockPort = mock(DistributedLockPort.class);
        seatRepositoryPort = mock(SeatRepositoryPort.class);
        reservationRepositoryPort = mock(ReservationRepositoryPort.class);
        handler = new HoldSeatCommandHandler(distributedLockPort, seatRepositoryPort, reservationRepositoryPort);
    }

    @Test
    @DisplayName("Should successfully hold seat when lock acquired and seat available")
    void shouldHoldSeatSuccessfully() {
        String eventId = "evt_001";
        String seatId = "seat_001";
        String userId = "user_001";
        HoldSeatCommand command = new HoldSeatCommand(eventId, seatId, userId);

        when(distributedLockPort.acquireSeatLock(eq(eventId), eq(seatId), eq(userId), any(Duration.class)))
                .thenReturn(true);

        Seat seat = Seat.createNew(SeatId.of(seatId), eventId, "sec_1", "A", "1", Money.of(new BigDecimal("100"), "USD"));
        when(seatRepositoryPort.findByEventIdAndSeatId(eventId, seatId)).thenReturn(Optional.of(seat));

        when(reservationRepositoryPort.saveWithOutbox(any(TicketReservation.class), any(Seat.class), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResult result = handler.handle(command);

        assertNotNull(result);
        assertEquals(eventId, result.eventId());
        assertEquals(seatId, result.seatId());
        assertEquals(userId, result.userId());
        assertEquals(ReservationStatus.PENDING_PAYMENT, result.status());
        assertEquals(SeatStatus.HELD, seat.getStatus());
        verify(reservationRepositoryPort, times(1)).saveWithOutbox(any(), eq(seat), any());
    }

    @Test
    @DisplayName("Should immediately throw SeatAlreadyHeldException if lock acquisition fails")
    void shouldThrowWhenLockFails() {
        String eventId = "evt_001";
        String seatId = "seat_001";
        String userId = "user_001";
        HoldSeatCommand command = new HoldSeatCommand(eventId, seatId, userId);

        when(distributedLockPort.acquireSeatLock(eq(eventId), eq(seatId), eq(userId), any(Duration.class)))
                .thenReturn(false);

        assertThrows(SeatAlreadyHeldException.class, () -> handler.handle(command));
        verifyNoInteractions(seatRepositoryPort);
        verifyNoInteractions(reservationRepositoryPort);
    }

    @Test
    @DisplayName("Should release lock and throw SeatNotFoundException if seat absent in repository")
    void shouldReleaseLockWhenSeatNotFound() {
        String eventId = "evt_001";
        String seatId = "seat_999";
        String userId = "user_001";
        HoldSeatCommand command = new HoldSeatCommand(eventId, seatId, userId);

        when(distributedLockPort.acquireSeatLock(eq(eventId), eq(seatId), eq(userId), any(Duration.class)))
                .thenReturn(true);
        when(seatRepositoryPort.findByEventIdAndSeatId(eventId, seatId)).thenReturn(Optional.empty());
        when(seatRepositoryPort.findById(SeatId.of(seatId))).thenReturn(Optional.empty());

        assertThrows(SeatNotFoundException.class, () -> handler.handle(command));
        verify(distributedLockPort, times(1)).releaseSeatLock(eventId, seatId, userId);
        verifyNoInteractions(reservationRepositoryPort);
    }

    @Test
    @DisplayName("Should auto-expire prior hold and succeed when prior reservation is expired (passive eviction deadlock remediation)")
    void shouldAutoExpirePriorHoldAndSucceedWhenPriorReservationExpired() {
        String eventId = "evt_001";
        String seatId = "seat_001";
        String newUserId = "user_002";
        HoldSeatCommand command = new HoldSeatCommand(eventId, seatId, newUserId);

        when(distributedLockPort.acquireSeatLock(eq(eventId), eq(seatId), eq(newUserId), any(Duration.class)))
                .thenReturn(true);

        Seat seat = Seat.createNew(SeatId.of(seatId), eventId, "sec_1", "A", "1", Money.of(new BigDecimal("100"), "USD"));
        seat.hold("prior_user"); // Seat is currently HELD

        // Expired reservation in past
        TicketReservation expiredReservation = TicketReservation.reconstitute(
                com.aieta.ticketing_app_inventory.domain.model.ReservationId.generate(),
                eventId,
                seat.getId(),
                "prior_user",
                ReservationStatus.PENDING_PAYMENT,
                Instant.now().minusSeconds(10), // expired 10s ago
                Instant.now().minusSeconds(610),
                null,
                null
        );

        when(seatRepositoryPort.findByEventIdAndSeatId(eventId, seatId)).thenReturn(Optional.of(seat));
        when(reservationRepositoryPort.findByEventIdAndSeatId(eventId, seat.getId())).thenReturn(Optional.of(expiredReservation));
        when(reservationRepositoryPort.saveWithOutbox(any(TicketReservation.class), any(Seat.class), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResult result = handler.handle(command);

        assertNotNull(result);
        assertEquals(newUserId, result.userId());
        assertEquals(ReservationStatus.EXPIRED, expiredReservation.getStatus());
        verify(reservationRepositoryPort, times(1)).save(expiredReservation);
        verify(reservationRepositoryPort, times(1)).saveWithOutbox(any(), eq(seat), any());
    }

    @Test
    @DisplayName("Should reject and release caller lock when seat is HELD by an active unexpired reservation")
    void shouldThrowSeatAlreadyHeldAndReleaseCallerLockWhenPriorReservationStillActive() {
        String eventId = "evt_001";
        String seatId = "seat_001";
        String newUserId = "user_002";
        HoldSeatCommand command = new HoldSeatCommand(eventId, seatId, newUserId);

        when(distributedLockPort.acquireSeatLock(eq(eventId), eq(seatId), eq(newUserId), any(Duration.class)))
                .thenReturn(true);

        Seat seat = Seat.createNew(SeatId.of(seatId), eventId, "sec_1", "A", "1", Money.of(new BigDecimal("100"), "USD"));
        seat.hold("prior_user");

        // Active reservation expiring in future
        TicketReservation activeReservation = TicketReservation.createNew(
                eventId,
                seat.getId(),
                "prior_user",
                Duration.ofSeconds(300)
        );

        when(seatRepositoryPort.findByEventIdAndSeatId(eventId, seatId)).thenReturn(Optional.of(seat));
        when(reservationRepositoryPort.findByEventIdAndSeatId(eventId, seat.getId())).thenReturn(Optional.of(activeReservation));

        assertThrows(SeatAlreadyHeldException.class, () -> handler.handle(command));
        verify(distributedLockPort, times(1)).releaseSeatLock(eventId, seatId, newUserId);
    }

    @Test
    @DisplayName("Should release caller lock on any exception during saveWithOutbox persistence")
    void shouldReleaseCallerLockWhenSaveWithOutboxFails() {
        String eventId = "evt_001";
        String seatId = "seat_001";
        String userId = "user_001";
        HoldSeatCommand command = new HoldSeatCommand(eventId, seatId, userId);

        when(distributedLockPort.acquireSeatLock(eq(eventId), eq(seatId), eq(userId), any(Duration.class)))
                .thenReturn(true);

        Seat seat = Seat.createNew(SeatId.of(seatId), eventId, "sec_1", "A", "1", Money.of(new BigDecimal("100"), "USD"));
        when(seatRepositoryPort.findByEventIdAndSeatId(eventId, seatId)).thenReturn(Optional.of(seat));
        when(reservationRepositoryPort.saveWithOutbox(any(), any(), any()))
                .thenThrow(new RuntimeException("Simulated Outbox Failure"));

        assertThrows(RuntimeException.class, () -> handler.handle(command));
        verify(distributedLockPort, times(1)).releaseSeatLock(eventId, seatId, userId);
    }
}
