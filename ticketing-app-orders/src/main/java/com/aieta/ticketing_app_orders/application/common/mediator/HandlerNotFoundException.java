package com.aieta.ticketing_app_orders.application.common.mediator;

public class HandlerNotFoundException extends RuntimeException {
    public HandlerNotFoundException(Class<?> requestType) {
        super("No handler registered for request type: " + requestType.getName());
    }
}
