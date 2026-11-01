package com.aieta.ticketing_app_catalog.domain.exceptions;

public class DuplicateEventSlugException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DuplicateEventSlugException(String slug) {
        super("Event with slug '" + slug + "' already exists");
    }
}
