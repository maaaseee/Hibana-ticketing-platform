package com.aieta.ticketing_service.domain.ports;

import com.aieta.ticketing_service.domain.model.Seat;
import com.aieta.ticketing_service.domain.model.SeatId;

import java.util.List;
import java.util.Optional;

public interface SeatRepositoryPort {
    Optional<Seat> findById(SeatId seatId);
    Optional<Seat> findByEventIdAndSeatId(String eventId, String seatId);
    List<Seat> findByEventId(String eventId);
    List<Seat> findByEventIdAndSectorId(String eventId, String sectorId);
    Seat save(Seat seat);
    void saveAll(List<Seat> seats);
    long countByEventId(String eventId);
}
