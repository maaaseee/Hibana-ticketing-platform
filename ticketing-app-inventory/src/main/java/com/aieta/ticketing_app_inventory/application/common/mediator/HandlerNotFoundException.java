package com.aieta.ticketing_app_inventory.application.common.mediator;

public class HandlerNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public HandlerNotFoundException(Class<?> requestType) {
        super(String.format("No request handler registered for request type: %s", requestType.getName()));
    }
}
