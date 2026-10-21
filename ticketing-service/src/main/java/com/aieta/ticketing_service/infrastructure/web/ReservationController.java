package com.aieta.ticketing_service.infrastructure.web;

import com.aieta.ticketing_service.application.commands.confirm_reservation.ConfirmReservationCommand;
import com.aieta.ticketing_service.application.commands.confirm_reservation.ConfirmationResult;
import com.aieta.ticketing_service.application.commands.hold_seat.HoldSeatCommand;
import com.aieta.ticketing_service.application.commands.hold_seat.ReservationResult;
import com.aieta.ticketing_service.application.commands.release_seat.ReleaseSeatCommand;
import com.aieta.ticketing_service.application.common.mediator.Mediator;
import com.aieta.ticketing_service.domain.model.ReservationId;
import com.aieta.ticketing_service.domain.model.TicketReservation;
import com.aieta.ticketing_service.domain.ports.ReservationRepositoryPort;
import com.aieta.ticketing_service.infrastructure.web.dto.ConfirmReservationRequest;
import com.aieta.ticketing_service.infrastructure.web.dto.ConfirmReservationResponse;
import com.aieta.ticketing_service.infrastructure.web.dto.HoldSeatRequest;
import com.aieta.ticketing_service.infrastructure.web.dto.HoldSeatResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final Mediator mediator;
    private final ReservationRepositoryPort reservationRepositoryPort;

    public ReservationController(Mediator mediator, ReservationRepositoryPort reservationRepositoryPort) {
        this.mediator = mediator;
        this.reservationRepositoryPort = reservationRepositoryPort;
    }

    @PostMapping
    public ResponseEntity<HoldSeatResponse> holdSeat(@Valid @RequestBody HoldSeatRequest request) {
        HoldSeatCommand command = new HoldSeatCommand(
                request.eventId(),
                request.seatId(),
                request.userId()
        );

        ReservationResult result = mediator.dispatch(command);

        HoldSeatResponse response = new HoldSeatResponse(
                result.reservationId(),
                result.eventId(),
                result.seatId(),
                result.userId(),
                result.status(),
                result.expiresAt(),
                result.countdownSeconds()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{reservationId}/confirm")
    public ResponseEntity<ConfirmReservationResponse> confirmReservation(
            @PathVariable("reservationId") String reservationId,
            @RequestBody(required = false) ConfirmReservationRequest request
    ) {
        String orderId = (request != null && request.orderId() != null && !request.orderId().isBlank())
                ? request.orderId()
                : "ord_" + reservationId;

        ConfirmReservationCommand command = new ConfirmReservationCommand(reservationId, orderId);
        ConfirmationResult result = mediator.dispatch(command);

        ConfirmReservationResponse response = new ConfirmReservationResponse(
                result.reservationId(),
                result.eventId(),
                result.seatId(),
                result.status(),
                result.orderId(),
                result.confirmedAt()
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{reservationId}")
    public ResponseEntity<Void> releaseReservation(
            @PathVariable("reservationId") String reservationId,
            @RequestParam(value = "reason", required = false, defaultValue = "USER_CANCELLED") String reason
    ) {
        Optional<TicketReservation> reservationOpt = reservationRepositoryPort.findById(ReservationId.of(reservationId));
        if (reservationOpt.isPresent()) {
            TicketReservation res = reservationOpt.get();
            ReleaseSeatCommand command = new ReleaseSeatCommand(res.getEventId(), res.getSeatId().value(), reason);
            mediator.dispatch(command);
        }
        return ResponseEntity.noContent().build();
    }
}
