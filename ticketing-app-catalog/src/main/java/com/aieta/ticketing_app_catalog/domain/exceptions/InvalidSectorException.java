package com.aieta.ticketing_app_catalog.domain.exceptions;

public class InvalidSectorException extends RuntimeException {
    public InvalidSectorException(String message) {
        super(message);
    }
}
