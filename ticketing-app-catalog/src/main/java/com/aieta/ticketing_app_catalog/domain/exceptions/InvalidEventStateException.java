package com.aieta.ticketing_app_catalog.domain.exceptions;

public class InvalidEventStateException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidEventStateException(String message) {
        super(message);
    }
}
