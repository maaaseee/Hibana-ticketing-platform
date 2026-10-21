package com.aieta.ticketing_app_catalog.domain.exceptions;

public class InvalidEventIdentifierException extends RuntimeException {
    public InvalidEventIdentifierException(String message) {
        super(message);
    }
}
