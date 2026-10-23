package com.aieta.ticketing_app_catalog.infrastructure;

import com.aieta.ticketing_app_catalog.domain.entity.Event;
import com.aieta.ticketing_app_catalog.domain.entity.EventSector;
import com.aieta.ticketing_app_catalog.domain.entity.EventStatus;
import com.aieta.ticketing_app_catalog.domain.event.EventCreatedEvent;
import com.aieta.ticketing_app_catalog.domain.port.EventWriteRepositoryPort;
import com.aieta.ticketing_app_catalog.infrastructure.database.entity.OutboxEventJpaEntity;
import com.aieta.ticketing_app_catalog.infrastructure.database.repository.SpringDataOutboxEventRepository;
import com.aieta.ticketing_app_catalog.infrastructure.messaging.outbox.OutboxRelayScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Integration Test: Real Database Event Write & Transactional Outbox")
class EventWriteRepositoryIT {

    @Autowired
    private EventWriteRepositoryPort writeRepository;

    @Autowired
    private SpringDataOutboxEventRepository outboxRepository;

    @Autowired
    private OutboxRelayScheduler outboxRelayScheduler;

    @Test
    @DisplayName("Should persist Event, child sectors, and Outbox event in a single local ACID transaction")
    void shouldPersistEventAndOutboxAtomically() {
        String eventId = "evt_it_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String slug = "test-event-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Instant now = Instant.now();

        EventSector sector = new EventSector(
                "sec_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                "Platea Central",
                "GENERAL_ADMISSION",
                250,
                new BigDecimal("65.00"),
                new BigDecimal("7.50"),
                "ARS"
        );

        Event event = Event.createNew(
                eventId,
                "org_amaryllis_live",
                "ven_estadio_nacional",
                slug,
                "Integration Test Concert",
                "CONCERT",
                List.of("ROCK"),
                now.plus(5, ChronoUnit.DAYS),
                now.plus(5, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS),
                now.plus(5, ChronoUnit.DAYS).plus(5, ChronoUnit.HOURS),
                "America/Santiago",
                "{}",
                "{}",
                List.of(sector)
        );

        EventCreatedEvent createdEvent = new EventCreatedEvent(
                eventId,
                "org_amaryllis_live",
                "ven_estadio_nacional",
                slug,
                "Integration Test Concert",
                now.plus(5, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS),
                Collections.emptyList(),
                Instant.now()
        );

        Event saved = writeRepository.saveWithOutbox(event, createdEvent);
        assertNotNull(saved);
        assertEquals(eventId, saved.getId());
        assertEquals(EventStatus.DRAFT, saved.getStatus());

        assertTrue(writeRepository.existsBySlug(slug));
        assertTrue(writeRepository.venueExists("ven_estadio_nacional"));

        Optional<Event> reloaded = writeRepository.findByIdForUpdate(eventId);
        assertTrue(reloaded.isPresent());
        assertEquals(1, reloaded.get().getSectors().size());
        assertEquals("Platea Central", reloaded.get().getSectors().get(0).getName());

        List<OutboxEventJpaEntity> pendingEvents = outboxRepository.findPendingEvents(PageRequest.of(0, 50));
        assertFalse(pendingEvents.isEmpty(), "Outbox record must exist with status PENDING");

        OutboxEventJpaEntity matchingOutbox = pendingEvents.stream()
                .filter(o -> eventId.equals(o.getAggregateId()))
                .findFirst()
                .orElse(null);

        assertNotNull(matchingOutbox, "Outbox event for created event must be present");
        assertEquals("Event", matchingOutbox.getAggregateType());
        assertEquals("EventCreatedEvent", matchingOutbox.getEventType());
        assertEquals("PENDING", matchingOutbox.getStatus());
        assertTrue(matchingOutbox.getPayload().contains(slug));

        outboxRelayScheduler.relayPendingEvents();

        Optional<OutboxEventJpaEntity> updatedOutbox = outboxRepository.findById(matchingOutbox.getId());
        assertTrue(updatedOutbox.isPresent());
        assertEquals("PUBLISHED", updatedOutbox.get().getStatus(), "Outbox event must transition to PUBLISHED after relay");
        assertNotNull(updatedOutbox.get().getProcessedAt());
    }
}
