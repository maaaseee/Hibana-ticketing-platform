package com.aieta.ticketing_app_catalog.domain.exceptions;

public class EventNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public EventNotFoundException(String eventId) {
        super("Event not found with identifier: " + eventId);
    }
}
