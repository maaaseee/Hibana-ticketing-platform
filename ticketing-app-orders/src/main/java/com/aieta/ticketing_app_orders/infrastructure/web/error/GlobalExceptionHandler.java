package com.aieta.ticketing_app_orders.infrastructure.web.error;

import com.aieta.ticketing_app_orders.application.common.mediator.HandlerNotFoundException;
import com.aieta.ticketing_app_orders.domain.exceptions.DomainException;
import com.aieta.ticketing_app_orders.domain.exceptions.InsufficientFundsException;
import com.aieta.ticketing_app_orders.domain.exceptions.InvalidOrderStateTransitionException;
import com.aieta.ticketing_app_orders.domain.exceptions.LedgerImbalanceException;
import com.aieta.ticketing_app_orders.domain.exceptions.OrderExpiredException;
import com.aieta.ticketing_app_orders.domain.exceptions.OrderNotFoundException;
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

    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail handleOrderNotFound(OrderNotFoundException ex) {
        log.warn("Order not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Order Not Found");
        problem.setType(URI.create("https://hibana.tk/errors/order-not-found"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "ORDER_NOT_FOUND");
        problem.setProperty("errorCode", "ORDER_NOT_FOUND");
        problem.setProperty("orderId", ex.getOrderId());
        return problem;
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ProblemDetail handleInsufficientFunds(InsufficientFundsException ex) {
        log.warn("Insufficient wallet funds: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.PAYMENT_REQUIRED, ex.getMessage());
        problem.setTitle("Insufficient Funds");
        problem.setType(URI.create("https://hibana.tk/errors/insufficient-funds"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "INSUFFICIENT_FUNDS");
        problem.setProperty("errorCode", "INSUFFICIENT_FUNDS");
        problem.setProperty("available", ex.getAvailable());
        problem.setProperty("required", ex.getRequired());
        return problem;
    }

    @ExceptionHandler(InvalidOrderStateTransitionException.class)
    public ProblemDetail handleInvalidStateTransition(InvalidOrderStateTransitionException ex) {
        log.warn("Invalid order state transition: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
        problem.setTitle("Invalid Order State Transition");
        problem.setType(URI.create("https://hibana.tk/errors/invalid-order-state"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "INVALID_ORDER_STATE_TRANSITION");
        problem.setProperty("errorCode", "INVALID_ORDER_STATE_TRANSITION");
        problem.setProperty("currentStatus", ex.getCurrentStatus());
        problem.setProperty("targetStatus", ex.getTargetStatus());
        return problem;
    }

    @ExceptionHandler(OrderExpiredException.class)
    public ProblemDetail handleOrderExpired(OrderExpiredException ex) {
        log.warn("Order expired: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.GONE, ex.getMessage());
        problem.setTitle("Order Expired");
        problem.setType(URI.create("https://hibana.tk/errors/order-expired"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "ORDER_EXPIRED");
        problem.setProperty("errorCode", "ORDER_EXPIRED");
        problem.setProperty("orderId", ex.getOrderId());
        return problem;
    }

    @ExceptionHandler(LedgerImbalanceException.class)
    public ProblemDetail handleLedgerImbalance(LedgerImbalanceException ex) {
        log.error("CRITICAL: Ledger double-entry imbalance: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        problem.setTitle("Ledger Imbalance Invariant Violation");
        problem.setType(URI.create("https://hibana.tk/errors/ledger-imbalance"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "LEDGER_IMBALANCE");
        problem.setProperty("errorCode", "LEDGER_IMBALANCE");
        problem.setProperty("totalDebits", ex.getTotalDebits());
        problem.setProperty("totalCredits", ex.getTotalCredits());
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
        log.warn("Validation failure: {}", ex.getMessage());
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

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException ex) {
        log.warn("Domain business rule violation: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Domain Business Rule Violation");
        problem.setType(URI.create("https://hibana.tk/errors/domain-violation"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", "DOMAIN_RULE_VIOLATION");
        problem.setProperty("errorCode", "DOMAIN_RULE_VIOLATION");
        return problem;
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        log.warn("Access denied on orders endpoint: {}", ex.getMessage());
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
