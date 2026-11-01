package com.aieta.ticketing_app_catalog.domain.exceptions;

public class EventAlreadyCancelledException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public EventAlreadyCancelledException(String eventId) {
        super("Event '" + eventId + "' is already cancelled and cannot be mutated");
    }
}
