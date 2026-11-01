package com.aieta.ticketing_app_catalog.domain.exceptions;

public class InvalidEventIdentifierException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidEventIdentifierException(String message) {
        super(message);
    }
}
