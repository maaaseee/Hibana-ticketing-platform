package com.aieta.ticketing_service.application.commands.hold_seat;

import com.aieta.ticketing_service.application.common.mediator.RequestHandler;
import com.aieta.ticketing_service.domain.events.SeatHeldEvent;
import com.aieta.ticketing_service.domain.exceptions.SeatAlreadyHeldException;
import com.aieta.ticketing_service.domain.exceptions.SeatNotFoundException;
import com.aieta.ticketing_service.domain.model.Seat;
import com.aieta.ticketing_service.domain.model.SeatId;
import com.aieta.ticketing_service.domain.model.TicketReservation;
import com.aieta.ticketing_service.domain.ports.DistributedLockPort;
import com.aieta.ticketing_service.domain.ports.ReservationRepositoryPort;
import com.aieta.ticketing_service.domain.ports.SeatRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
public class HoldSeatCommandHandler implements RequestHandler<HoldSeatCommand, ReservationResult> {
    private static final Logger log = LoggerFactory.getLogger(HoldSeatCommandHandler.class);

    private final DistributedLockPort distributedLockPort;
    private final SeatRepositoryPort seatRepositoryPort;
    private final ReservationRepositoryPort reservationRepositoryPort;

    public HoldSeatCommandHandler(
            DistributedLockPort distributedLockPort,
            SeatRepositoryPort seatRepositoryPort,
            ReservationRepositoryPort reservationRepositoryPort
    ) {
        this.distributedLockPort = distributedLockPort;
        this.seatRepositoryPort = seatRepositoryPort;
        this.reservationRepositoryPort = reservationRepositoryPort;
    }

    @Override
    public ReservationResult handle(HoldSeatCommand command) {
        Duration ttl = command.holdDuration();
        boolean lockAcquired = distributedLockPort.acquireSeatLock(
                command.eventId(),
                command.seatId(),
                command.userId(),
                ttl
        );

        if (!lockAcquired) {
            log.warn("Failed to acquire Redis Redlock for event {} and seat {}", command.eventId(), command.seatId());
            throw new SeatAlreadyHeldException(command.eventId(), command.seatId());
        }

        try {
            Seat seat = seatRepositoryPort.findByEventIdAndSeatId(command.eventId(), command.seatId())
                    .or(() -> seatRepositoryPort.findById(SeatId.of(command.seatId())))
                    .orElseThrow(() -> new SeatNotFoundException(command.eventId(), command.seatId()));

            // Handle passive eviction race condition: Seat is HELD in DB but reservation expired
            if (seat.isHeld()) {
                Optional<TicketReservation> existingResOpt = reservationRepositoryPort
                        .findByEventIdAndSeatId(command.eventId(), seat.getId());

                if (existingResOpt.isPresent()) {
                    TicketReservation existingRes = existingResOpt.get();
                    if (existingRes.isExpired()) {
                        log.info("Seat {} is HELD by expired reservation {}. Auto-releasing prior hold for user {}",
                                seat.getId().value(), existingRes.getId().value(), command.userId());
                        existingRes.expire();
                        reservationRepositoryPort.save(existingRes);
                        seat.release();
                    } else {
                        // Truly active hold by another user
                        throw new SeatAlreadyHeldException(command.eventId(), command.seatId());
                    }
                } else {
                    // Orphaned HELD state without active reservation
                    log.warn("Seat {} was HELD with no reservation found. Releasing orphaned hold.", seat.getId().value());
                    seat.release();
                }
            }

            seat.hold(command.userId());

            TicketReservation reservation = TicketReservation.createNew(
                    command.eventId(),
                    seat.getId(),
                    command.userId(),
                    ttl
            );

            SeatHeldEvent event = SeatHeldEvent.of(
                    reservation.getId().value(),
                    command.eventId(),
                    seat.getId().value(),
                    command.userId(),
                    reservation.getExpiresAt()
            );

            TicketReservation savedReservation = reservationRepositoryPort.saveWithOutbox(reservation, seat, event);

            long countdownSeconds = Duration.between(Instant.now(), savedReservation.getExpiresAt()).toSeconds();
            if (countdownSeconds < 0) {
                countdownSeconds = 0;
            }

            return new ReservationResult(
                    savedReservation.getId().value(),
                    savedReservation.getEventId(),
                    savedReservation.getSeatId().value(),
                    savedReservation.getUserId(),
                    savedReservation.getStatus(),
                    savedReservation.getExpiresAt(),
                    countdownSeconds
            );
        } catch (Exception ex) {
            // Relinquish lock acquired by this caller on ANY exception
            distributedLockPort.releaseSeatLock(command.eventId(), command.seatId(), command.userId());
            throw ex;
        }
    }

    @Override
    public Class<HoldSeatCommand> getRequestType() {
        return HoldSeatCommand.class;
    }
}
