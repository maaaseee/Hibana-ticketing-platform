package com.aieta.ticketing_app_catalog.common.mediator;

public class HandlerNotFoundException extends RuntimeException {
    public HandlerNotFoundException(Class<?> requestType) {
        super("No handler found for request type: " + requestType.getName());
    }
}
