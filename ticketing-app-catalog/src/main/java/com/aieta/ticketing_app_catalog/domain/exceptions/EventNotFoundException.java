package com.aieta.ticketing_app_catalog.domain.exceptions;

public class EventNotFoundException extends RuntimeException {
    public EventNotFoundException(String eventId) {
        super("Event not found with identifier: " + eventId);
    }
}
