package com.aieta.ticketing_app_catalog.domain.exceptions;

public class InvalidEventStateException extends RuntimeException {
    public InvalidEventStateException(String message) {
        super(message);
    }
}
