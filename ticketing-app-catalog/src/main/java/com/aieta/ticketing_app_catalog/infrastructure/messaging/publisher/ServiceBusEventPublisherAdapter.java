package com.aieta.ticketing_app_catalog.infrastructure.messaging.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ServiceBusEventPublisherAdapter implements EventBusPublisher {

    private static final Logger log = LoggerFactory.getLogger(ServiceBusEventPublisherAdapter.class);

    @Override
    public void publish(String topic, String messageId, String payload) {
        log.info("Dispatched domain event to topic [{}]: messageId={}, payload={}", topic, messageId, payload);
    }
}
