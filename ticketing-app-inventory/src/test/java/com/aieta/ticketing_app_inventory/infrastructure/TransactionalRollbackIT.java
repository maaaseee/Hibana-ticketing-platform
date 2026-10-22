package com.aieta.ticketing_app_inventory.infrastructure;

import com.aieta.ticketing_app_inventory.domain.enums.SeatStatus;
import com.aieta.ticketing_app_inventory.domain.model.Money;
import com.aieta.ticketing_app_inventory.domain.model.Seat;
import com.aieta.ticketing_app_inventory.domain.model.SeatId;
import com.aieta.ticketing_app_inventory.domain.model.TicketReservation;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.adapter.JpaReservationRepositoryAdapter;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.entity.SeatJpaEntity;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.mapper.SeatMapper;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.repository.SpringDataOutboxRepository;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.repository.SpringDataReservationRepository;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.repository.SpringDataSeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Integration Test: Transactional Dual-Write Rollback Guarantee")
class TransactionalRollbackIT {

    @Autowired
    private SpringDataSeatRepository seatRepository;

    @Autowired
    private SpringDataReservationRepository reservationRepository;

    @MockitoBean
    private SpringDataOutboxRepository outboxRepository;

    @Autowired
    private JpaReservationRepositoryAdapter reservationRepositoryAdapter;

    @Autowired
    private SeatMapper seatMapper;

    @BeforeEach
    void cleanUp() {
        reservationRepository.deleteAll();
        seatRepository.deleteAll();
    }

    @Test
    @DisplayName("Should rollback seat reservation and seat status completely when outbox persistence fails")
    void shouldRollbackReservationWhenOutboxFails() {
        // 1. Arrange: Seed initial seat in AVAILABLE status
        String seatId = "seat_rollback_test_01";
        String eventId = "evt_rollback_01";
        Seat seat = Seat.createNew(
                SeatId.of(seatId),
                eventId,
                "sec_1",
                "A",
                "1",
                Money.of(new BigDecimal("100.00"), "USD")
        );
        seatRepository.save(seatMapper.toEntity(seat));

        // 2. Configure @MockitoBean outboxRepository to throw RuntimeException
        doThrow(new RuntimeException("Simulated Database I/O / Outbox Failure"))
                .when(outboxRepository).save(any());

        // 3. Prepare reservation and mutate seat domain state to HELD
        seat.hold("user_fail_01");
        TicketReservation reservation = TicketReservation.createNew(
                eventId,
                seat.getId(),
                "user_fail_01",
                Duration.ofSeconds(600)
        );

        // 4. Act: Invoke saveWithOutbox via Spring's @Transactional proxy
        assertThrows(RuntimeException.class, () ->
                reservationRepositoryAdapter.saveWithOutbox(reservation, seat, "test_event")
        );

        // 5. Assert: Verify genuine transactional rollback across both entities
        // A) Reservation entity must NOT exist in database
        assertFalse(reservationRepository.findById(reservation.getId().value()).isPresent(),
                "Reservation must NOT be committed if Outbox dual-write fails");

        // B) Seat entity in DB must remain AVAILABLE (rolled back from HELD)
        SeatJpaEntity persistedSeat = seatRepository.findById(seatId).orElseThrow();
        assertEquals(SeatStatus.AVAILABLE.name(), persistedSeat.getStatus(),
                "Seat status in DB must roll back to AVAILABLE when transaction aborts");
    }
}
