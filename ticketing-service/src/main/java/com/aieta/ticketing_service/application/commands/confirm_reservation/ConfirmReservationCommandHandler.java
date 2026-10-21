package com.aieta.ticketing_service.application.commands.confirm_reservation;

import com.aieta.ticketing_service.application.common.mediator.RequestHandler;
import com.aieta.ticketing_service.domain.events.ReservationConfirmedEvent;
import com.aieta.ticketing_service.domain.exceptions.ReservationExpiredException;
import com.aieta.ticketing_service.domain.exceptions.ReservationNotFoundException;
import com.aieta.ticketing_service.domain.exceptions.SeatNotFoundException;
import com.aieta.ticketing_service.domain.model.ReservationId;
import com.aieta.ticketing_service.domain.model.Seat;
import com.aieta.ticketing_service.domain.model.TicketReservation;
import com.aieta.ticketing_service.domain.ports.ReservationRepositoryPort;
import com.aieta.ticketing_service.domain.ports.SeatRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ConfirmReservationCommandHandler implements RequestHandler<ConfirmReservationCommand, ConfirmationResult> {
    private static final Logger log = LoggerFactory.getLogger(ConfirmReservationCommandHandler.class);

    private final ReservationRepositoryPort reservationRepositoryPort;
    private final SeatRepositoryPort seatRepositoryPort;

    public ConfirmReservationCommandHandler(
            ReservationRepositoryPort reservationRepositoryPort,
            SeatRepositoryPort seatRepositoryPort
    ) {
        this.reservationRepositoryPort = reservationRepositoryPort;
        this.seatRepositoryPort = seatRepositoryPort;
    }

    @Override
    public ConfirmationResult handle(ConfirmReservationCommand command) {
        TicketReservation reservation = reservationRepositoryPort.findById(ReservationId.of(command.reservationId()))
                .orElseThrow(() -> new ReservationNotFoundException(command.reservationId()));

        if (reservation.isExpired()) {
            reservation.expire();
            reservationRepositoryPort.save(reservation);
            log.warn("Attempt to confirm expired reservation {}", command.reservationId());
            throw new ReservationExpiredException(reservation.getId().value(), reservation.getExpiresAt());
        }

        Seat seat = seatRepositoryPort.findById(reservation.getSeatId())
                .orElseThrow(() -> new SeatNotFoundException(reservation.getSeatId().value()));

        Instant now = Instant.now();
        reservation.confirm(command.orderId(), now);
        seat.book();

        ReservationConfirmedEvent event = ReservationConfirmedEvent.of(
                reservation.getId().value(),
                reservation.getEventId(),
                seat.getId().value(),
                command.orderId()
        );

        TicketReservation savedReservation = reservationRepositoryPort.saveWithOutbox(reservation, seat, event);

        return new ConfirmationResult(
                savedReservation.getId().value(),
                savedReservation.getEventId(),
                savedReservation.getSeatId().value(),
                savedReservation.getStatus(),
                savedReservation.getOrderId(),
                savedReservation.getConfirmedAt()
        );
    }

    @Override
    public Class<ConfirmReservationCommand> getRequestType() {
        return ConfirmReservationCommand.class;
    }
}
