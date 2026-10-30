package com.aieta.ticketing_app_orders.domain.exceptions;

import com.aieta.ticketing_app_orders.domain.enums.OrderStatus;

public class InvalidOrderStateTransitionException extends DomainException {
    private final OrderStatus currentStatus;
    private final OrderStatus targetStatus;

    public InvalidOrderStateTransitionException(OrderStatus currentStatus, OrderStatus targetStatus) {
        super("Cannot transition order from " + currentStatus + " to " + targetStatus);
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }

    public OrderStatus getTargetStatus() {
        return targetStatus;
    }
}
