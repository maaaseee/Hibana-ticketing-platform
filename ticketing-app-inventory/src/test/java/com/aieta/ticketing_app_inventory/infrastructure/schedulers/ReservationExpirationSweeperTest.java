package com.aieta.ticketing_app_inventory.infrastructure.schedulers;

import com.aieta.ticketing_app_inventory.domain.enums.ReservationStatus;
import com.aieta.ticketing_app_inventory.domain.enums.SeatStatus;
import com.aieta.ticketing_app_inventory.domain.ports.DistributedLockPort;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.entity.SeatJpaEntity;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.entity.TicketReservationJpaEntity;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.repository.SpringDataReservationRepository;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.repository.SpringDataSeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("Unit Test: ReservationExpirationSweeper")
class ReservationExpirationSweeperTest {

    private SpringDataReservationRepository reservationRepository;
    private SpringDataSeatRepository seatRepository;
    private DistributedLockPort distributedLockPort;
    private ReservationExpirationSweeper sweeper;

    @BeforeEach
    void setUp() {
        reservationRepository = mock(SpringDataReservationRepository.class);
        seatRepository = mock(SpringDataSeatRepository.class);
        distributedLockPort = mock(DistributedLockPort.class);
        sweeper = new ReservationExpirationSweeper(reservationRepository, seatRepository, distributedLockPort, 50);
    }

    @Test
    @DisplayName("Should do nothing when no expired reservations exist")
    void shouldDoNothingWhenNoExpiredReservations() {
        when(reservationRepository.findExpiredReservations(eq("PENDING_PAYMENT"), any(Instant.class), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        sweeper.sweepExpiredReservations();

        verifyNoInteractions(seatRepository);
        verifyNoInteractions(distributedLockPort);
    }

    @Test
    @DisplayName("Should expire reservations, reset HELD seats to AVAILABLE, and purge Redis locks")
    void shouldSweepExpiredReservationsSuccessfully() {
        Instant past = Instant.now().minusSeconds(30);
        TicketReservationJpaEntity res = new TicketReservationJpaEntity(
                "res_001",
                "evt_001",
                "seat_001",
                "user_001",
                ReservationStatus.PENDING_PAYMENT.name(),
                past,
                past.minusSeconds(600),
                null,
                null
        );

        SeatJpaEntity seat = new SeatJpaEntity(
                "seat_001",
                "evt_001",
                "sec_1",
                "A",
                "1",
                new BigDecimal("100.00"),
                "USD",
                SeatStatus.HELD.name(),
                1L,
                past.minusSeconds(600),
                past
        );

        when(reservationRepository.findExpiredReservations(eq("PENDING_PAYMENT"), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(res));
        when(seatRepository.findById("seat_001")).thenReturn(Optional.of(seat));

        sweeper.sweepExpiredReservations();

        assertEquals(ReservationStatus.EXPIRED.name(), res.getStatus());
        verify(reservationRepository, times(1)).save(res);

        assertEquals(SeatStatus.AVAILABLE.name(), seat.getStatus());
        verify(seatRepository, times(1)).save(seat);

        verify(distributedLockPort, times(1)).forceReleaseSeatLock("evt_001", "seat_001");
    }

    @Test
    @DisplayName("Should not reset seat status if seat is not HELD (e.g. already BOOKED)")
    void shouldNotResetSeatIfNotHeld() {
        Instant past = Instant.now().minusSeconds(30);
        TicketReservationJpaEntity res = new TicketReservationJpaEntity(
                "res_002",
                "evt_001",
                "seat_002",
                "user_002",
                ReservationStatus.PENDING_PAYMENT.name(),
                past,
                past.minusSeconds(600),
                null,
                null
        );

        SeatJpaEntity seat = new SeatJpaEntity(
                "seat_002",
                "evt_001",
                "sec_1",
                "A",
                "2",
                new BigDecimal("100.00"),
                "USD",
                SeatStatus.BOOKED.name(),
                2L,
                past.minusSeconds(600),
                past
        );

        when(reservationRepository.findExpiredReservations(eq("PENDING_PAYMENT"), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(res));
        when(seatRepository.findById("seat_002")).thenReturn(Optional.of(seat));

        sweeper.sweepExpiredReservations();

        assertEquals(ReservationStatus.EXPIRED.name(), res.getStatus());
        verify(reservationRepository, times(1)).save(res);

        assertEquals(SeatStatus.BOOKED.name(), seat.getStatus());
        verify(seatRepository, never()).save(seat);

        verify(distributedLockPort, times(1)).forceReleaseSeatLock("evt_001", "seat_002");
    }
}
