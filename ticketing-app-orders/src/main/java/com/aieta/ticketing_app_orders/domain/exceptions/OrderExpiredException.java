package com.aieta.ticketing_app_orders.domain.exceptions;

public class OrderExpiredException extends DomainException {
    private static final long serialVersionUID = 1L;

    private final String orderId;

    public OrderExpiredException(String orderId) {
        super("Order has expired: " + orderId);
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }
}
