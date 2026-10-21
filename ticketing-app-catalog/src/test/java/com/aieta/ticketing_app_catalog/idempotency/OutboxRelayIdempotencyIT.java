package com.aieta.ticketing_app_catalog.idempotency;

import com.aieta.ticketing_app_catalog.infrastructure.database.entity.OutboxEventJpaEntity;
import com.aieta.ticketing_app_catalog.infrastructure.database.repository.SpringDataOutboxEventRepository;
import com.aieta.ticketing_app_catalog.infrastructure.messaging.outbox.OutboxRelayScheduler;
import com.aieta.ticketing_app_catalog.infrastructure.messaging.publisher.EventBusPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@DisplayName("Idempotency Test: Outbox Relay Retries & Deduplication (Milestone 5.5)")
class OutboxRelayIdempotencyIT {

    @Autowired
    private SpringDataOutboxEventRepository outboxRepository;

    @Autowired
    private OutboxRelayScheduler outboxRelayScheduler;

    @MockitoSpyBean
    private EventBusPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
        Mockito.reset(eventPublisher);
    }

    @Test
    @DisplayName("Should ensure 20 repeated relay executions publish exactly once with deterministic messageId evt_{outboxId}")
    void shouldRelayOutboxEventIdempotentlyAcrossRetries() {
        UUID outboxId = UUID.randomUUID();
        String expectedMessageId = "evt_" + outboxId;
        String payload = "{\"eventId\":\"evt_test_123\",\"slug\":\"test-slug\"}";

        OutboxEventJpaEntity outboxEvent = new OutboxEventJpaEntity(
                outboxId,
                "Event",
                "evt_test_123",
                "EventCreatedEvent",
                payload);
        outboxRepository.saveAndFlush(outboxEvent);

        // Execute relay scheduler 20 times consecutively
        final int retryCount = 20;
        for (int i = 0; i < retryCount; i++) {
            outboxRelayScheduler.relayPendingEvents();
        }

        // Verify outbox event transitioned to PUBLISHED and processedAt is recorded
        OutboxEventJpaEntity persisted = outboxRepository.findById(outboxId).orElseThrow();
        assertEquals("PUBLISHED", persisted.getStatus());
        assertNotNull(persisted.getProcessedAt());

        // Verify that across 20 invocations, publish was called EXACTLY ONCE with the
        // deterministic messageId
        verify(eventPublisher, times(1)).publish(
                eq("event.lifecycle.v1"),
                eq(expectedMessageId),
                argThat(p -> p != null && p.contains("evt_test_123") && p.contains("test-slug")));
        verifyNoMoreInteractions(eventPublisher);
    }
}
