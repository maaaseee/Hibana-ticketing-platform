package com.aieta.ticketing_app_catalog.infrastructure.api.exception;

import com.aieta.ticketing_app_catalog.common.mediator.HandlerNotFoundException;
import com.aieta.ticketing_app_catalog.domain.exceptions.*;
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

    @ExceptionHandler(EventNotFoundException.class)
    public ProblemDetail handleEventNotFound(EventNotFoundException ex) {
        log.warn("Event not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Event Not Found");
        problem.setType(URI.create("https://hibana.tk/errors/not-found"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "EVENT_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(InvalidEventScheduleException.class)
    public ProblemDetail handleInvalidSchedule(InvalidEventScheduleException ex) {
        log.warn("Invalid event schedule invariant: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
        problem.setTitle("Invalid Event Schedule");
        problem.setType(URI.create("https://hibana.tk/errors/invalid-schedule"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "INVALID_EVENT_SCHEDULE");
        return problem;
    }

    @ExceptionHandler(InvalidEventTitleException.class)
    public ProblemDetail handleInvalidTitle(InvalidEventTitleException ex) {
        log.warn("Invalid event title: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Event Title");
        problem.setType(URI.create("https://hibana.tk/errors/invalid-title"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "INVALID_EVENT_TITLE");
        return problem;
    }

    @ExceptionHandler(InvalidSectorPricingException.class)
    public ProblemDetail handleInvalidPricing(InvalidSectorPricingException ex) {
        log.warn("Invalid sector pricing invariant: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Sector Pricing");
        problem.setType(URI.create("https://hibana.tk/errors/invalid-pricing"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "INVALID_SECTOR_PRICING");
        return problem;
    }

    @ExceptionHandler(InvalidSectorCapacityException.class)
    public ProblemDetail handleInvalidCapacity(InvalidSectorCapacityException ex) {
        log.warn("Invalid sector capacity invariant: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Sector Capacity");
        problem.setType(URI.create("https://hibana.tk/errors/invalid-capacity"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "INVALID_SECTOR_CAPACITY");
        return problem;
    }

    @ExceptionHandler(InvalidEventIdentifierException.class)
    public ProblemDetail handleInvalidIdentifier(InvalidEventIdentifierException ex) {
        log.warn("Invalid event identifier supplied: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Event Identifier");
        problem.setType(URI.create("https://hibana.tk/errors/invalid-identifier"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "INVALID_EVENT_IDENTIFIER");
        return problem;
    }

    @ExceptionHandler(HandlerNotFoundException.class)
    public ProblemDetail handleMediatorHandlerNotFound(HandlerNotFoundException ex) {
        log.error("Mediator dispatch failure: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        problem.setTitle("CQRS Handler Not Found");
        problem.setType(URI.create("https://hibana.tk/errors/handler-not-found"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "MEDIATOR_HANDLER_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(VenueNotFoundException.class)
    public ProblemDetail handleVenueNotFound(VenueNotFoundException ex) {
        log.warn("Venue not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Venue Not Found");
        problem.setType(URI.create("https://hibana.tk/errors/venue-not-found"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "VENUE_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(DuplicateEventSlugException.class)
    public ProblemDetail handleDuplicateSlug(DuplicateEventSlugException ex) {
        log.warn("Duplicate event slug collision: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Duplicate Event Slug");
        problem.setType(URI.create("https://hibana.tk/errors/duplicate-slug"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "DUPLICATE_EVENT_SLUG");
        return problem;
    }

    @ExceptionHandler(EventAlreadyCancelledException.class)
    public ProblemDetail handleEventAlreadyCancelled(EventAlreadyCancelledException ex) {
        log.warn("Event already cancelled: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Event Already Cancelled");
        problem.setType(URI.create("https://hibana.tk/errors/already-cancelled"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "EVENT_ALREADY_CANCELLED");
        return problem;
    }

    @ExceptionHandler(InvalidEventStateException.class)
    public ProblemDetail handleInvalidEventState(InvalidEventStateException ex) {
        log.warn("Invalid event state transition: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
        problem.setTitle("Invalid Event State");
        problem.setType(URI.create("https://hibana.tk/errors/invalid-state"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "INVALID_EVENT_STATE");
        return problem;
    }

    @ExceptionHandler(InvalidSectorException.class)
    public ProblemDetail handleInvalidSector(InvalidSectorException ex) {
        log.warn("Invalid sector domain error: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Sector");
        problem.setType(URI.create("https://hibana.tk/errors/invalid-sector"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "INVALID_SECTOR");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        log.warn("Request payload validation error: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed for request content");
        problem.setTitle("Constraint Violation");
        problem.setType(URI.create("https://hibana.tk/errors/validation"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "VALIDATION_FAILED");
        problem.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .toList());
        return problem;
    }
}
