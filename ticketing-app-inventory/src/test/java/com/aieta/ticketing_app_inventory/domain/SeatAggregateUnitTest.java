package com.aieta.ticketing_app_inventory.domain;

import com.aieta.ticketing_app_inventory.domain.enums.SeatStatus;
import com.aieta.ticketing_app_inventory.domain.exceptions.InvalidSeatStateTransitionException;
import com.aieta.ticketing_app_inventory.domain.exceptions.SeatAlreadyHeldException;
import com.aieta.ticketing_app_inventory.domain.model.Money;
import com.aieta.ticketing_app_inventory.domain.model.Seat;
import com.aieta.ticketing_app_inventory.domain.model.SeatId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Test: Pure Seat Aggregate Invariants (Zero Spring/DB)")
class SeatAggregateUnitTest {

    private Seat createAvailableSeat(String seatId) {
        return Seat.createNew(
                SeatId.of(seatId),
                "evt_coldplay_2026",
                "sec_vip",
                "A",
                "104",
                Money.of(new BigDecimal("150.00"), "USD")
        );
    }

    @Test
    @DisplayName("Should create seat with initial status AVAILABLE")
    void shouldCreateSeatWithInitialAvailableStatus() {
        Seat seat = createAvailableSeat("seat_104");

        assertNotNull(seat);
        assertEquals("seat_104", seat.getId().value());
        assertEquals("evt_coldplay_2026", seat.getEventId());
        assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
        assertTrue(seat.isAvailable());
        assertFalse(seat.isHeld());
        assertFalse(seat.isBooked());
        assertEquals(new BigDecimal("150.00"), seat.getPrice().amount());
        assertEquals("USD", seat.getPrice().currency());
    }

    @Test
    @DisplayName("Should transition from AVAILABLE to HELD on hold()")
    void shouldTransitionFromAvailableToHeld() {
        Seat seat = createAvailableSeat("seat_104");
        seat.hold("user_123");

        assertEquals(SeatStatus.HELD, seat.getStatus());
        assertTrue(seat.isHeld());
        assertFalse(seat.isAvailable());
    }

    @Test
    @DisplayName("Should throw SeatAlreadyHeldException when holding an already HELD seat")
    void shouldThrowWhenHoldingAlreadyHeldSeat() {
        Seat seat = createAvailableSeat("seat_104");
        seat.hold("user_123");

        assertThrows(SeatAlreadyHeldException.class, () -> seat.hold("user_456"));
    }

    @Test
    @DisplayName("Should transition from HELD to BOOKED on book()")
    void shouldTransitionFromHeldToBooked() {
        Seat seat = createAvailableSeat("seat_104");
        seat.hold("user_123");
        seat.book();

        assertEquals(SeatStatus.BOOKED, seat.getStatus());
        assertTrue(seat.isBooked());
        assertFalse(seat.isHeld());
    }

    @Test
    @DisplayName("Should throw InvalidSeatStateTransitionException when booking an AVAILABLE seat directly")
    void shouldThrowWhenBookingAvailableSeatDirectly() {
        Seat seat = createAvailableSeat("seat_104");

        assertThrows(InvalidSeatStateTransitionException.class, seat::book);
    }

    @Test
    @DisplayName("Should release HELD seat back to AVAILABLE on release()")
    void shouldReleaseHeldSeatBackToAvailable() {
        Seat seat = createAvailableSeat("seat_104");
        seat.hold("user_123");
        seat.release();

        assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
        assertTrue(seat.isAvailable());
    }

    @Test
    @DisplayName("Should reject releasing an already BOOKED seat directly")
    void shouldRejectReleasingBookedSeat() {
        Seat seat = createAvailableSeat("seat_104");
        seat.hold("user_123");
        seat.book();

        assertThrows(InvalidSeatStateTransitionException.class, seat::release);
    }

    @Test
    @DisplayName("Should block and unblock AVAILABLE seat")
    void shouldBlockAndUnblockAvailableSeat() {
        Seat seat = createAvailableSeat("seat_104");
        seat.block();
        assertEquals(SeatStatus.BLOCKED, seat.getStatus());

        seat.unblock();
        assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
    }

    @Test
    @DisplayName("Should reject blocking a HELD seat")
    void shouldRejectBlockingHeldSeat() {
        Seat seat = createAvailableSeat("seat_104");
        seat.hold("user_123");

        assertThrows(InvalidSeatStateTransitionException.class, seat::block);
    }
}
