package com.aieta.ticketing_app_orders.infrastructure.outbox;

import com.aieta.ticketing_app_orders.domain.ports.EventBusPublisherPort;
import com.aieta.ticketing_app_orders.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.aieta.ticketing_app_orders.infrastructure.persistence.repository.SpringDataOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "ticketing.outbox.relay", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayScheduler {
    private static final Logger log = LoggerFactory.getLogger(OutboxRelayScheduler.class);

    private final SpringDataOutboxRepository outboxRepository;
    private final EventBusPublisherPort eventPublisher;
    private final String orderLifecycleTopic;
    private final int batchSize;

    public OutboxRelayScheduler(
            SpringDataOutboxRepository outboxRepository,
            EventBusPublisherPort eventPublisher,
            @Value("${ticketing.messaging.topics.order-lifecycle:order.lifecycle.v1}") String orderLifecycleTopic,
            @Value("${ticketing.outbox.relay.batch-size:50}") int batchSize
    ) {
        this.outboxRepository = outboxRepository;
        this.eventPublisher = eventPublisher;
        this.orderLifecycleTopic = orderLifecycleTopic;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${ticketing.outbox.relay.fixed-delay-ms:500}")
    @Transactional
    public void relayPendingEvents() {
        List<OutboxEventJpaEntity> pendingEvents;
        try {
            pendingEvents = outboxRepository.findPendingEventsForUpdateSkipLocked(batchSize);
        } catch (Exception ex) {
            log.trace("Native SKIP LOCKED query failed, falling back to standard pagination: {}", ex.getMessage());
            pendingEvents = outboxRepository.findPendingEvents(PageRequest.of(0, batchSize));
        }

        if (pendingEvents == null || pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Polling Orders Outbox: found {} pending events to relay", pendingEvents.size());

        for (OutboxEventJpaEntity event : pendingEvents) {
            try {
                String messageId = "evt_" + event.getId().toString();
                eventPublisher.publish(orderLifecycleTopic, messageId, event.getPayload());
                event.markAsPublished();
                outboxRepository.save(event);
                log.debug("Successfully relayed outbox event ID {} to topic {}", event.getId(), orderLifecycleTopic);
            } catch (Exception e) {
                log.error("Failed to relay outbox event ID {}: {}", event.getId(), e.getMessage(), e);
                event.markAsFailed();
                outboxRepository.save(event);
            }
        }
    }
}
