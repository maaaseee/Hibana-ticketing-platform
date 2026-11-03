package com.aieta.ticketing_app_inventory.infrastructure.web.error;

import com.aieta.ticketing_app_inventory.application.common.mediator.HandlerNotFoundException;
import com.aieta.ticketing_app_inventory.domain.exceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(SeatAlreadyHeldException.class)
    public ProblemDetail handleSeatAlreadyHeld(SeatAlreadyHeldException ex) {
        log.warn("Seat already held collision: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Seat Already Held");
        problem.setType(URI.create("https://hibana.tk/errors/seat-already-held"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "SEAT_ALREADY_HELD");
        problem.setProperty("errorCode", "SEAT_ALREADY_HELD");
        problem.setProperty("eventId", ex.getEventId());
        problem.setProperty("seatId", ex.getSeatId());
        return problem;
    }

    @ExceptionHandler(SeatNotFoundException.class)
    public ProblemDetail handleSeatNotFound(SeatNotFoundException ex) {
        log.warn("Seat not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Seat Not Found");
        problem.setType(URI.create("https://hibana.tk/errors/seat-not-found"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "SEAT_NOT_FOUND");
        problem.setProperty("errorCode", "SEAT_NOT_FOUND");
        problem.setProperty("seatId", ex.getSeatId());
        return problem;
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ProblemDetail handleReservationNotFound(ReservationNotFoundException ex) {
        log.warn("Reservation not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Reservation Not Found");
        problem.setType(URI.create("https://hibana.tk/errors/reservation-not-found"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "RESERVATION_NOT_FOUND");
        problem.setProperty("errorCode", "RESERVATION_NOT_FOUND");
        problem.setProperty("reservationId", ex.getReservationId());
        return problem;
    }

    @ExceptionHandler(ReservationExpiredException.class)
    public ProblemDetail handleReservationExpired(ReservationExpiredException ex) {
        log.warn("Reservation expired: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.GONE, ex.getMessage());
        problem.setTitle("Reservation Expired");
        problem.setType(URI.create("https://hibana.tk/errors/reservation-expired"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "RESERVATION_EXPIRED");
        problem.setProperty("errorCode", "RESERVATION_EXPIRED");
        problem.setProperty("reservationId", ex.getReservationId());
        problem.setProperty("expiredAt", ex.getExpiredAt());
        return problem;
    }

    @ExceptionHandler(InvalidSeatStateTransitionException.class)
    public ProblemDetail handleInvalidStateTransition(InvalidSeatStateTransitionException ex) {
        log.warn("Invalid seat state transition: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
        problem.setTitle("Invalid Seat State Transition");
        problem.setType(URI.create("https://hibana.tk/errors/invalid-state-transition"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "INVALID_SEAT_STATE_TRANSITION");
        problem.setProperty("errorCode", "INVALID_SEAT_STATE_TRANSITION");
        if (ex.getCurrentStatus() != null) {
            problem.setProperty("currentStatus", ex.getCurrentStatus());
        }
        if (ex.getTargetStatus() != null) {
            problem.setProperty("targetStatus", ex.getTargetStatus());
        }
        return problem;
    }

    @ExceptionHandler(HandlerNotFoundException.class)
    public ProblemDetail handleHandlerNotFound(HandlerNotFoundException ex) {
        log.error("Mediator dispatch failure: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        problem.setTitle("CQRS Handler Not Found");
        problem.setType(URI.create("https://hibana.tk/errors/handler-not-found"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "MEDIATOR_HANDLER_NOT_FOUND");
        problem.setProperty("errorCode", "MEDIATOR_HANDLER_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        log.warn("Request validation failure: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed for request content");
        problem.setTitle("Constraint Violation");
        problem.setType(URI.create("https://hibana.tk/errors/validation"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "VALIDATION_FAILED");
        problem.setProperty("errorCode", "VALIDATION_FAILED");
        problem.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .toList());
        return problem;
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        log.warn("Access denied on inventory endpoint: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Forbidden");
        problem.setType(URI.create("https://hibana.tk/errors/forbidden"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "ACCESS_DENIED");
        problem.setProperty("errorCode", "ACCESS_DENIED");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unhandled internal server error: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://hibana.tk/errors/internal-error"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "INTERNAL_SERVER_ERROR");
        problem.setProperty("errorCode", "INTERNAL_SERVER_ERROR");
        return problem;
    }
}
