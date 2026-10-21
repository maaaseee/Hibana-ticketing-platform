package com.aieta.ticketing_app_catalog.infrastructure.messaging.publisher;

public interface EventBusPublisher {
    void publish(String topic, String messageId, String payload);
}
