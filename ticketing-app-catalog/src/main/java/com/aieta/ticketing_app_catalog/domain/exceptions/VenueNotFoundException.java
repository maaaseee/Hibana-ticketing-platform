package com.aieta.ticketing_app_catalog.domain.exceptions;

public class VenueNotFoundException extends RuntimeException {
    public VenueNotFoundException(String venueId) {
        super("Venue not found with identifier: " + venueId);
    }
}
