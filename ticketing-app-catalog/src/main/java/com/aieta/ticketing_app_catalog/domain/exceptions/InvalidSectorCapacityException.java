package com.aieta.ticketing_app_catalog.domain.exceptions;

public class InvalidSectorCapacityException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidSectorCapacityException(int totalCapacity) {
        super("Total capacity must be strictly positive, but was: " + totalCapacity);
    }
}
