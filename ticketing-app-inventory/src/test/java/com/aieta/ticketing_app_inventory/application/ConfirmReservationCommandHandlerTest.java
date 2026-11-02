package com.aieta.ticketing_app_inventory.application;

import com.aieta.ticketing_app_inventory.application.commands.confirm_reservation.ConfirmReservationCommand;
import com.aieta.ticketing_app_inventory.application.commands.confirm_reservation.ConfirmReservationCommandHandler;
import com.aieta.ticketing_app_inventory.application.commands.confirm_reservation.ConfirmationResult;
import com.aieta.ticketing_app_inventory.domain.enums.ReservationStatus;
import com.aieta.ticketing_app_inventory.domain.enums.SeatStatus;
import com.aieta.ticketing_app_inventory.domain.exceptions.ReservationExpiredException;
import com.aieta.ticketing_app_inventory.domain.exceptions.ReservationNotFoundException;
import com.aieta.ticketing_app_inventory.domain.model.Money;
import com.aieta.ticketing_app_inventory.domain.model.ReservationId;
import com.aieta.ticketing_app_inventory.domain.model.Seat;
import com.aieta.ticketing_app_inventory.domain.model.SeatId;
import com.aieta.ticketing_app_inventory.domain.model.TicketReservation;
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

@DisplayName("Unit Test: ConfirmReservationCommandHandler")
class ConfirmReservationCommandHandlerTest {

    private ReservationRepositoryPort reservationRepositoryPort;
    private SeatRepositoryPort seatRepositoryPort;
    private ConfirmReservationCommandHandler handler;

    @BeforeEach
    void setUp() {
        reservationRepositoryPort = mock(ReservationRepositoryPort.class);
        seatRepositoryPort = mock(SeatRepositoryPort.class);
        handler = new ConfirmReservationCommandHandler(reservationRepositoryPort, seatRepositoryPort);
    }

    @Test
    @DisplayName("Should successfully confirm valid pending reservation")
    void shouldConfirmReservationSuccessfully() {
        String resId = "res_1234567890123456";
        String eventId = "evt_001";
        String seatId = "seat_001";
        String orderId = "ord_999";

        TicketReservation reservation = TicketReservation.createNew(eventId, SeatId.of(seatId), "user_1", Duration.ofSeconds(600));
        Seat seat = Seat.createNew(SeatId.of(seatId), eventId, "sec_1", "A", "1", Money.of(new BigDecimal("15000.00"), "ARS"));
        seat.hold("user_1");

        when(reservationRepositoryPort.findById(ReservationId.of(resId))).thenReturn(Optional.of(reservation));
        when(seatRepositoryPort.findById(reservation.getSeatId())).thenReturn(Optional.of(seat));
        when(reservationRepositoryPort.saveWithOutbox(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        ConfirmReservationCommand command = new ConfirmReservationCommand(resId, orderId);
        ConfirmationResult result = handler.handle(command);

        assertNotNull(result);
        assertEquals(ReservationStatus.CONFIRMED, result.status());
        assertEquals(orderId, result.orderId());
        assertEquals(SeatStatus.BOOKED, seat.getStatus());
        verify(reservationRepositoryPort, times(1)).saveWithOutbox(eq(reservation), eq(seat), any());
    }

    @Test
    @DisplayName("Should throw ReservationNotFoundException when reservation does not exist")
    void shouldThrowWhenReservationNotFound() {
        when(reservationRepositoryPort.findById(ReservationId.of("res_missing"))).thenReturn(Optional.empty());

        ConfirmReservationCommand command = new ConfirmReservationCommand("res_missing", "ord_1");
        assertThrows(ReservationNotFoundException.class, () -> handler.handle(command));
    }

    @Test
    @DisplayName("Should throw ReservationExpiredException when reservation has elapsed hold duration")
    void shouldThrowWhenReservationIsExpired() {
        String resId = "res_expired";
        TicketReservation expiredRes = TicketReservation.reconstitute(
                ReservationId.of(resId),
                "evt_001",
                SeatId.of("seat_001"),
                "user_1",
                ReservationStatus.PENDING_PAYMENT,
                Instant.now().minusSeconds(10), // in the past!
                Instant.now().minusSeconds(610),
                null,
                null
        );

        when(reservationRepositoryPort.findById(ReservationId.of(resId))).thenReturn(Optional.of(expiredRes));

        ConfirmReservationCommand command = new ConfirmReservationCommand(resId, "ord_1");
        assertThrows(ReservationExpiredException.class, () -> handler.handle(command));
        verify(reservationRepositoryPort, times(1)).save(expiredRes);
    }

    @Test
    @DisplayName("Should return existing result idempotently when reservation is already CONFIRMED")
    void shouldReturnExistingResultWhenAlreadyConfirmed() {
        String resId = "res_already_confirmed";
        Instant confirmedTime = Instant.now().minusSeconds(30);
        TicketReservation confirmedRes = TicketReservation.reconstitute(
                ReservationId.of(resId),
                "evt_001",
                SeatId.of("seat_001"),
                "user_1",
                ReservationStatus.CONFIRMED,
                Instant.now().plusSeconds(300),
                Instant.now().minusSeconds(300),
                confirmedTime,
                "ord_existing"
        );

        when(reservationRepositoryPort.findById(ReservationId.of(resId))).thenReturn(Optional.of(confirmedRes));

        ConfirmReservationCommand command = new ConfirmReservationCommand(resId, "ord_existing");
        ConfirmationResult result = handler.handle(command);

        assertNotNull(result);
        assertEquals(ReservationStatus.CONFIRMED, result.status());
        assertEquals("ord_existing", result.orderId());
        assertEquals(confirmedTime, result.confirmedAt());
        verify(reservationRepositoryPort, never()).saveWithOutbox(any(), any(), any());
    }
}
