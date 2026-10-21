package com.aieta.ticketing_app_catalog.infrastructure.messaging.outbox;

import com.aieta.ticketing_app_catalog.infrastructure.database.entity.OutboxEventJpaEntity;
import com.aieta.ticketing_app_catalog.infrastructure.database.repository.SpringDataOutboxEventRepository;
import com.aieta.ticketing_app_catalog.infrastructure.messaging.publisher.EventBusPublisher;
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
@ConditionalOnProperty(prefix = "catalog.outbox.relay", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayScheduler.class);

    private final SpringDataOutboxEventRepository outboxRepository;
    private final EventBusPublisher eventPublisher;
    private final String eventLifecycleTopic;
    private final int batchSize;

    public OutboxRelayScheduler(
            SpringDataOutboxEventRepository outboxRepository,
            EventBusPublisher eventPublisher,
            @Value("${catalog.messaging.topics.event-lifecycle:event.lifecycle.v1}") String eventLifecycleTopic,
            @Value("${catalog.outbox.relay.batch-size:50}") int batchSize) {
        this.outboxRepository = outboxRepository;
        this.eventPublisher = eventPublisher;
        this.eventLifecycleTopic = eventLifecycleTopic;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${catalog.outbox.relay.fixed-delay-ms:500}")
    @Transactional
    public void relayPendingEvents() {
        List<OutboxEventJpaEntity> pendingEvents = outboxRepository.findPendingEvents(PageRequest.of(0, batchSize));

        if (pendingEvents.isEmpty()) {
            return;
        }

        for (OutboxEventJpaEntity event : pendingEvents) {
            try {
                String messageId = "evt_" + event.getId().toString();
                eventPublisher.publish(eventLifecycleTopic, messageId, event.getPayload());
                event.markAsPublished();
                outboxRepository.save(event);
                log.debug("Successfully relayed outbox event ID: {}", event.getId());
            } catch (Exception e) {
                log.error("Failed to relay outbox event ID: {}", event.getId(), e);
                event.markAsFailed();
                outboxRepository.save(event);
            }
        }
    }
}
