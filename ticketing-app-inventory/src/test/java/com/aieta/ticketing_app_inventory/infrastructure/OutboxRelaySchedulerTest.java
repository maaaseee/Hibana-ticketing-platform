package com.aieta.ticketing_app_inventory.infrastructure;

import com.aieta.ticketing_app_inventory.domain.ports.EventBusPublisherPort;
import com.aieta.ticketing_app_inventory.infrastructure.outbox.OutboxRelayScheduler;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.aieta.ticketing_app_inventory.infrastructure.persistence.repository.SpringDataOutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("Unit Test: OutboxRelayScheduler Lifecycle State Machine")
class OutboxRelaySchedulerTest {

        @Test
        @DisplayName("Should poll pending outbox events, publish to Service Bus, and mark as PUBLISHED")
        void shouldRelayPendingEventsAndMarkPublished() {
                SpringDataOutboxRepository outboxRepo = mock(SpringDataOutboxRepository.class);
                EventBusPublisherPort eventPublisher = mock(EventBusPublisherPort.class);

                OutboxRelayScheduler scheduler = new OutboxRelayScheduler(
                                outboxRepo,
                                eventPublisher,
                                "ticket.reservation.v1",
                                50);

                UUID eventId = UUID.randomUUID();
                OutboxEventJpaEntity pendingEvent = new OutboxEventJpaEntity(
                                eventId,
                                "TicketReservation",
                                "res_123",
                                "SeatHeldEvent",
                                "{\"reservationId\": \"res_123\"}");

                when(outboxRepo.findPendingEventsForUpdateSkipLocked(50)).thenReturn(List.of(pendingEvent));

                scheduler.relayPendingEvents();

                verify(eventPublisher, times(1)).publish(
                                eq("ticket.reservation.v1"),
                                eq("evt_" + eventId),
                                eq("{\"reservationId\": \"res_123\"}"));

                ArgumentCaptor<OutboxEventJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
                verify(outboxRepo, times(1)).save(captor.capture());

                OutboxEventJpaEntity savedEvent = captor.getValue();
                assertEquals("PUBLISHED", savedEvent.getStatus());
                assertNotNull(savedEvent.getProcessedAt());
        }

        @Test
        @DisplayName("Should increment retry_count and eventually mark FAILED on publisher error")
        void shouldHandlePublishingFailureAndMarkFailed() {
                SpringDataOutboxRepository outboxRepo = mock(SpringDataOutboxRepository.class);
                EventBusPublisherPort eventPublisher = mock(EventBusPublisherPort.class);

                OutboxRelayScheduler scheduler = new OutboxRelayScheduler(
                                outboxRepo,
                                eventPublisher,
                                "ticket.reservation.v1",
                                50);

                UUID eventId = UUID.randomUUID();
                OutboxEventJpaEntity pendingEvent = new OutboxEventJpaEntity(
                                eventId,
                                "TicketReservation",
                                "res_123",
                                "SeatHeldEvent",
                                "{}");
                pendingEvent.setRetryCount(2); // Next failure is 3rd failure

                when(outboxRepo.findPendingEventsForUpdateSkipLocked(50)).thenReturn(List.of(pendingEvent));
                doThrow(new RuntimeException("Service Bus Connection Unavailable"))
                                .when(eventPublisher).publish(any(), any(), any());

                scheduler.relayPendingEvents();

                ArgumentCaptor<OutboxEventJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
                verify(outboxRepo, times(1)).save(captor.capture());

                OutboxEventJpaEntity savedEvent = captor.getValue();
                assertEquals("FAILED", savedEvent.getStatus());
                assertEquals(3, savedEvent.getRetryCount());
        }
}
