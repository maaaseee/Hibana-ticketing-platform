package com.aieta.ticketing_app_catalog.domain.exceptions;

public class EventAlreadyCancelledException extends RuntimeException {
    public EventAlreadyCancelledException(String eventId) {
        super("Event '" + eventId + "' is already cancelled and cannot be mutated");
    }
}
