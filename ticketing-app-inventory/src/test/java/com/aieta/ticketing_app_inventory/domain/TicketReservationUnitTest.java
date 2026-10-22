package com.aieta.ticketing_app_inventory.domain;

import com.aieta.ticketing_app_inventory.domain.enums.ReservationStatus;
import com.aieta.ticketing_app_inventory.domain.exceptions.InvalidSeatStateTransitionException;
import com.aieta.ticketing_app_inventory.domain.exceptions.ReservationExpiredException;
import com.aieta.ticketing_app_inventory.domain.model.SeatId;
import com.aieta.ticketing_app_inventory.domain.model.TicketReservation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Test: Pure TicketReservation Aggregate Invariants (Zero Spring/DB)")
class TicketReservationUnitTest {

    @Test
    @DisplayName("Should create reservation with 600s TTL and status PENDING_PAYMENT")
    void shouldCreateReservationWithDefaultTTL() {
        TicketReservation reservation = TicketReservation.createNew(
                "evt_coldplay_2026",
                SeatId.of("seat_104"),
                "user_999",
                Duration.ofSeconds(600)
        );

        assertNotNull(reservation);
        assertNotNull(reservation.getId());
        assertEquals("evt_coldplay_2026", reservation.getEventId());
        assertEquals("seat_104", reservation.getSeatId().value());
        assertEquals("user_999", reservation.getUserId());
        assertEquals(ReservationStatus.PENDING_PAYMENT, reservation.getStatus());
        assertFalse(reservation.isExpired());
        assertTrue(reservation.getExpiresAt().isAfter(reservation.getCreatedAt()));
    }

    @Test
    @DisplayName("Should confirm pending reservation with orderId before expiration")
    void shouldConfirmPendingReservation() {
        TicketReservation reservation = TicketReservation.createNew(
                "evt_coldplay_2026",
                SeatId.of("seat_104"),
                "user_999",
                Duration.ofSeconds(600)
        );

        Instant confirmTime = reservation.getCreatedAt().plusSeconds(60);
        reservation.confirm("ord_777", confirmTime);

        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals("ord_777", reservation.getOrderId());
        assertEquals(confirmTime, reservation.getConfirmedAt());
    }

    @Test
    @DisplayName("Should reject confirmation and throw ReservationExpiredException when hold duration elapsed")
    void shouldRejectConfirmationWhenExpired() {
        TicketReservation reservation = TicketReservation.createNew(
                "evt_coldplay_2026",
                SeatId.of("seat_104"),
                "user_999",
                Duration.ofSeconds(600)
        );

        Instant pastExpirationTime = reservation.getCreatedAt().plusSeconds(601);

        assertThrows(ReservationExpiredException.class, () ->
                reservation.confirm("ord_777", pastExpirationTime)
        );
        assertEquals(ReservationStatus.EXPIRED, reservation.getStatus());
    }

    @Test
    @DisplayName("Should cancel pending reservation")
    void shouldCancelPendingReservation() {
        TicketReservation reservation = TicketReservation.createNew(
                "evt_coldplay_2026",
                SeatId.of("seat_104"),
                "user_999",
                Duration.ofSeconds(600)
        );

        reservation.cancel();
        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
    }

    @Test
    @DisplayName("Should reject cancellation of already CONFIRMED reservation")
    void shouldRejectCancellationOfConfirmedReservation() {
        TicketReservation reservation = TicketReservation.createNew(
                "evt_coldplay_2026",
                SeatId.of("seat_104"),
                "user_999",
                Duration.ofSeconds(600)
        );

        reservation.confirm("ord_777", reservation.getCreatedAt().plusSeconds(30));
        assertThrows(InvalidSeatStateTransitionException.class, reservation::cancel);
    }
}
