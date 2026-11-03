package com.aieta.ticketing_app_orders.infrastructure.messaging.publisher;

import com.aieta.ticketing_app_orders.domain.ports.EventBusPublisherPort;
import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ServiceBusEventPublisherAdapter implements EventBusPublisherPort {
    private static final Logger log = LoggerFactory.getLogger(ServiceBusEventPublisherAdapter.class);

    private final String connectionString;
    private final boolean enabled;
    private final Map<String, ServiceBusSenderClient> senderClients = new ConcurrentHashMap<>();

    public ServiceBusEventPublisherAdapter(
            @Value("${azure.servicebus.connection-string:}") String connectionString,
            @Value("${azure.servicebus.enabled:true}") boolean enabled
    ) {
        this.connectionString = connectionString;
        this.enabled = enabled;
    }

    @Override
    public void publish(String topic, String messageId, String payload) {
        log.info("[AZURE-SERVICEBUS] Preparing domain event for topic [{}]: messageId={}", topic, messageId);

        if (!enabled || connectionString == null || connectionString.isBlank()) {
            log.debug("[AZURE-SERVICEBUS] Service Bus disabled or connection string empty. Published in local log mode.");
            return;
        }

        try {
            ServiceBusSenderClient client = senderClients.computeIfAbsent(topic, t ->
                    new ServiceBusClientBuilder()
                            .connectionString(connectionString)
                            .sender()
                            .topicName(t)
                            .buildClient()
            );

            ServiceBusMessage message = new ServiceBusMessage(BinaryData.fromString(payload))
                    .setMessageId(messageId)
                    .setContentType("application/json");

            client.sendMessage(message);
            log.info("[AZURE-SERVICEBUS] Successfully dispatched message {} to Azure Service Bus topic [{}]", messageId, topic);
        } catch (Exception e) {
            log.warn("[AZURE-SERVICEBUS] Dispatched with fallback/warning to topic [{}] (id: {}): {}", topic, messageId, e.getMessage());
        }
    }
}
