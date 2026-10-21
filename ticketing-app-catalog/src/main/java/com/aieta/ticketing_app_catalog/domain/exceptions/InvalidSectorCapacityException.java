package com.aieta.ticketing_app_catalog.domain.exceptions;

public class InvalidSectorCapacityException extends RuntimeException {
    public InvalidSectorCapacityException(int totalCapacity) {
        super("Total capacity must be strictly positive, but was: " + totalCapacity);
    }
}
