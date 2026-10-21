package com.aieta.ticketing_service.application.queries.get_event_seats;

import com.aieta.ticketing_service.application.common.mediator.RequestHandler;
import com.aieta.ticketing_service.domain.enums.SeatStatus;
import com.aieta.ticketing_service.domain.model.Seat;
import com.aieta.ticketing_service.domain.ports.SeatRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GetEventSeatsQueryHandler implements RequestHandler<GetEventSeatsQuery, EventSeatsResponse> {

    private final SeatRepositoryPort seatRepositoryPort;

    public GetEventSeatsQueryHandler(SeatRepositoryPort seatRepositoryPort) {
        this.seatRepositoryPort = seatRepositoryPort;
    }

    @Override
    public EventSeatsResponse handle(GetEventSeatsQuery query) {
        List<Seat> seats;
        if (query.sectorId().isPresent() && !query.sectorId().get().isBlank()) {
            seats = seatRepositoryPort.findByEventIdAndSectorId(query.eventId(), query.sectorId().get());
        } else {
            seats = seatRepositoryPort.findByEventId(query.eventId());
        }

        int totalSeats = seats.size();
        int availableSeats = 0;
        int heldSeats = 0;
        int bookedSeats = 0;
        int blockedSeats = 0;

        List<SeatResponseDto> dtoList = seats.stream()
                .map(seat -> new SeatResponseDto(
                        seat.getId().value(),
                        seat.getEventId(),
                        seat.getSectorId(),
                        seat.getRowIdentifier(),
                        seat.getSeatNumber(),
                        seat.getPrice().amount(),
                        seat.getPrice().currency(),
                        seat.getStatus()
                ))
                .toList();

        for (Seat seat : seats) {
            switch (seat.getStatus()) {
                case AVAILABLE -> availableSeats++;
                case HELD -> heldSeats++;
                case BOOKED -> bookedSeats++;
                case BLOCKED -> blockedSeats++;
            }
        }

        return new EventSeatsResponse(
                query.eventId(),
                totalSeats,
                availableSeats,
                heldSeats,
                bookedSeats,
                blockedSeats,
                dtoList
        );
    }

    @Override
    public Class<GetEventSeatsQuery> getRequestType() {
        return GetEventSeatsQuery.class;
    }
}
