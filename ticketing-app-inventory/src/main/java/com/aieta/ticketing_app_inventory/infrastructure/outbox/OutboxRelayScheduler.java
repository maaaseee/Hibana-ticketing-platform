package com.aieta.ticketing_app_inventory.infrastructure.outbox;

import com.aieta.ticketing_app_inventory.domain.ports.EventBusPublisherPort;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.repository.SpringDataOutboxRepository;
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
    private final String ticketReservationTopic;
    private final int batchSize;

    public OutboxRelayScheduler(
            SpringDataOutboxRepository outboxRepository,
            EventBusPublisherPort eventPublisher,
            @Value("${ticketing.messaging.topics.ticket-reservation:ticket.reservation.v1}") String ticketReservationTopic,
            @Value("${ticketing.outbox.relay.batch-size:50}") int batchSize
    ) {
        this.outboxRepository = outboxRepository;
        this.eventPublisher = eventPublisher;
        this.ticketReservationTopic = ticketReservationTopic;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${ticketing.outbox.relay.fixed-delay-ms:500}")
    @Transactional
    public void relayPendingEvents() {
        List<OutboxEventJpaEntity> pendingEvents;
        try {
            // Attempt PostgreSQL FOR UPDATE SKIP LOCKED
            pendingEvents = outboxRepository.findPendingEventsForUpdateSkipLocked(batchSize);
        } catch (Exception ex) {
            // Fallback for non-Postgres / H2 in-memory test databases
            log.trace("Native SKIP LOCKED query failed, falling back to standard pagination: {}", ex.getMessage());
            pendingEvents = outboxRepository.findPendingEvents(PageRequest.of(0, batchSize));
        }

        if (pendingEvents == null || pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Polling Outbox: found {} pending events to relay", pendingEvents.size());

        for (OutboxEventJpaEntity event : pendingEvents) {
            try {
                String messageId = "evt_" + event.getId().toString();
                eventPublisher.publish(ticketReservationTopic, messageId, event.getPayload());
                event.markAsPublished();
                outboxRepository.save(event);
                log.debug("Successfully relayed outbox event ID {} to topic {}", event.getId(), ticketReservationTopic);
            } catch (Exception e) {
                log.error("Failed to relay outbox event ID {}: {}", event.getId(), e.getMessage(), e);
                event.markAsFailed();
                outboxRepository.save(event);
            }
        }
    }
}
