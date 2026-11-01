package com.aieta.ticketing_app_orders.domain.exceptions;

public class OrderNotFoundException extends DomainException {
    private static final long serialVersionUID = 1L;

    private final String orderId;

    public OrderNotFoundException(String orderId) {
        super("Order not found with id: " + orderId);
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }
}
