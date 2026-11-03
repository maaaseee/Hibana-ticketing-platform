package com.aieta.ticketing_app_inventory.infrastructure.messaging.amqp;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "azure.servicebus.enabled", havingValue = "true", matchIfMissing = true)
public class AzureServiceBusSubscriptionReceiver {
    private static final Logger log = LoggerFactory.getLogger(AzureServiceBusSubscriptionReceiver.class);

    private final EventCreatedAmqpListener eventCreatedListener;
    private final String connectionString;
    private final String topicName;
    private final String subscriptionName;
    private ServiceBusProcessorClient processorClient;

    public AzureServiceBusSubscriptionReceiver(
            EventCreatedAmqpListener eventCreatedListener,
            @Value("${azure.servicebus.connection-string:}") String connectionString,
            @Value("${ticketing.messaging.topics.event-lifecycle:event.lifecycle.v1}") String topicName,
            @Value("${ticketing.messaging.subscriptions.seat-generator:ticketing-seat-generator}") String subscriptionName
    ) {
        this.eventCreatedListener = eventCreatedListener;
        this.connectionString = connectionString;
        this.topicName = topicName;
        this.subscriptionName = subscriptionName;
    }

    @PostConstruct
    public void startListening() {
        if (connectionString == null || connectionString.isBlank()) {
            log.info("[AZURE-SERVICEBUS-RECEIVER] No connection string provided; listener not started (normal for unit tests).");
            return;
        }

        try {
            log.info("[AZURE-SERVICEBUS-RECEIVER] Initializing Azure Service Bus processor for topic [{}] sub [{}]", topicName, subscriptionName);
            processorClient = new ServiceBusClientBuilder()
                    .connectionString(connectionString)
                    .processor()
                    .topicName(topicName)
                    .subscriptionName(subscriptionName)
                    .processMessage(context -> {
                        String body = context.getMessage().getBody().toString();
                        log.info("[AZURE-SERVICEBUS-RECEIVER] Received message id: {}", context.getMessage().getMessageId());
                        eventCreatedListener.onEventCreatedMessage(body);
                    })
                    .processError(context -> {
                        log.warn("[AZURE-SERVICEBUS-RECEIVER] Subscription warning (emulator or network): {}", context.getException().getMessage());
                    })
                    .buildProcessorClient();

            processorClient.start();
            log.info("[AZURE-SERVICEBUS-RECEIVER] Started listening on Azure Service Bus topic [{}] sub [{}]", topicName, subscriptionName);
        } catch (Exception e) {
            log.warn("[AZURE-SERVICEBUS-RECEIVER] Could not start Service Bus processor (will retry or continue): {}", e.getMessage());
        }
    }

    @PreDestroy
    public void stopListening() {
        if (processorClient != null) {
            try {
                processorClient.close();
            } catch (Exception ignored) {}
        }
    }
}
