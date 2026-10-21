package com.aieta.ticketing_service.domain.ports;

public interface EventBusPublisherPort {
    void publish(String topic, String messageId, String payload);
}
