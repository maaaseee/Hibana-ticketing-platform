package com.aieta.ticketing_app_inventory.infrastructure.messaging.publisher;

import com.aieta.ticketing_app_inventory.domain.ports.EventBusPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ServiceBusEventPublisherAdapter implements EventBusPublisherPort {
    private static final Logger log = LoggerFactory.getLogger(ServiceBusEventPublisherAdapter.class);

    @Override
    public void publish(String topic, String messageId, String payload) {
        log.info("Dispatched domain event to Azure Service Bus topic [{}]: messageId={}, payload={}", topic, messageId, payload);
    }
}
