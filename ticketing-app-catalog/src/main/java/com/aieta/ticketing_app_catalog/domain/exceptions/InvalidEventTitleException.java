package com.aieta.ticketing_app_catalog.domain.exceptions;

public class InvalidEventTitleException extends RuntimeException {
    public InvalidEventTitleException() {
        super("Event title cannot be null or blank");
    }

    public InvalidEventTitleException(String message) {
        super(message);
    }
}
