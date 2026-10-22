package com.aieta.ticketing_app_inventory.application.commands.release_seat;

import com.aieta.ticketing_app_inventory.application.common.mediator.RequestHandler;
import com.aieta.ticketing_app_inventory.domain.events.SeatReleasedEvent;
import com.aieta.ticketing_app_inventory.domain.model.Seat;
import com.aieta.ticketing_app_inventory.domain.model.SeatId;
import com.aieta.ticketing_app_inventory.domain.model.TicketReservation;
import com.aieta.ticketing_app_inventory.domain.ports.DistributedLockPort;
import com.aieta.ticketing_app_inventory.domain.ports.ReservationRepositoryPort;
import com.aieta.ticketing_app_inventory.domain.ports.SeatRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ReleaseSeatCommandHandler implements RequestHandler<ReleaseSeatCommand, Void> {
    private static final Logger log = LoggerFactory.getLogger(ReleaseSeatCommandHandler.class);

    private final DistributedLockPort distributedLockPort;
    private final SeatRepositoryPort seatRepositoryPort;
    private final ReservationRepositoryPort reservationRepositoryPort;

    public ReleaseSeatCommandHandler(
            DistributedLockPort distributedLockPort,
            SeatRepositoryPort seatRepositoryPort,
            ReservationRepositoryPort reservationRepositoryPort
    ) {
        this.distributedLockPort = distributedLockPort;
        this.seatRepositoryPort = seatRepositoryPort;
        this.reservationRepositoryPort = reservationRepositoryPort;
    }

    @Override
    public Void handle(ReleaseSeatCommand command) {
        log.info("Releasing seat {} for event {} with reason {}", command.seatId(), command.eventId(), command.reason());

        // Active compensation: delete key in Redis
        distributedLockPort.forceReleaseSeatLock(command.eventId(), command.seatId());

        Optional<Seat> seatOpt = seatRepositoryPort.findByEventIdAndSeatId(command.eventId(), command.seatId())
                .or(() -> seatRepositoryPort.findById(SeatId.of(command.seatId())));

        if (seatOpt.isPresent()) {
            Seat seat = seatOpt.get();
            if (seat.isHeld()) {
                seat.release();
            }

            Optional<TicketReservation> resOpt = reservationRepositoryPort.findByEventIdAndSeatId(command.eventId(), seat.getId());
            SeatReleasedEvent event = SeatReleasedEvent.of(command.seatId(), command.eventId(), command.reason());

            if (resOpt.isPresent()) {
                TicketReservation reservation = resOpt.get();
                reservation.cancel();
                reservationRepositoryPort.saveWithOutbox(reservation, seat, event);
            } else {
                seatRepositoryPort.save(seat);
            }
        }

        return null;
    }

    @Override
    public Class<ReleaseSeatCommand> getRequestType() {
        return ReleaseSeatCommand.class;
    }
}
