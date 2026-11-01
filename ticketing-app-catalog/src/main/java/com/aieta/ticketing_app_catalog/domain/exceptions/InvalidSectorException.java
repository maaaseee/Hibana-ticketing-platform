package com.aieta.ticketing_app_catalog.domain.exceptions;

public class InvalidSectorException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidSectorException(String message) {
        super(message);
    }
}
