package com.aieta.ticketing_app_inventory.domain.ports;

public interface EventBusPublisherPort {
    void publish(String topic, String messageId, String payload);
}
