package com.aieta.ticketing_app_orders.application.common.mediator;

public class HandlerNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public HandlerNotFoundException(Class<?> requestType) {
        super("No handler registered for request type: " + requestType.getName());
    }
}
