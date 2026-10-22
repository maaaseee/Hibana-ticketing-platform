package com.aieta.ticketing_app_inventory.application.common.mediator;

public class HandlerNotFoundException extends RuntimeException {
    public HandlerNotFoundException(Class<?> requestType) {
        super(String.format("No request handler registered for request type: %s", requestType.getName()));
    }
}
