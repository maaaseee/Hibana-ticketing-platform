package com.aieta.ticketing_app_inventory.domain.ports;

import com.aieta.ticketing_app_inventory.domain.model.ReservationId;
import com.aieta.ticketing_app_inventory.domain.model.Seat;
import com.aieta.ticketing_app_inventory.domain.model.SeatId;
import com.aieta.ticketing_app_inventory.domain.model.TicketReservation;

import java.util.Optional;

public interface ReservationRepositoryPort {
    Optional<TicketReservation> findById(ReservationId reservationId);
    Optional<TicketReservation> findByEventIdAndSeatId(String eventId, SeatId seatId);
    TicketReservation saveWithOutbox(TicketReservation reservation, Seat seat, Object domainEvent);
    TicketReservation save(TicketReservation reservation);
    long countBySeatId(String seatId);
}
