package com.aieta.ticketing_app_inventory.domain.exceptions;

import com.aieta.ticketing_app_inventory.domain.enums.SeatStatus;

public class InvalidSeatStateTransitionException extends DomainException {
    private final SeatStatus currentStatus;
    private final SeatStatus targetStatus;

    public InvalidSeatStateTransitionException(SeatStatus currentStatus, SeatStatus targetStatus) {
        super(String.format("Invalid seat state transition from '%s' to '%s'", currentStatus, targetStatus));
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public InvalidSeatStateTransitionException(String message) {
        super(message);
        this.currentStatus = null;
        this.targetStatus = null;
    }

    public SeatStatus getCurrentStatus() {
        return currentStatus;
    }

    public SeatStatus getTargetStatus() {
        return targetStatus;
    }
}
