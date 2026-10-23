package com.aieta.ticketing_app_inventory.infrastructure.messaging.amqp;

import com.aieta.ticketing_app_inventory.application.commands.generate_seats.BatchGenerateSeatsCommand;
import com.aieta.ticketing_app_inventory.application.commands.generate_seats.SectorDefinition;
import com.aieta.ticketing_app_inventory.application.common.mediator.Mediator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EventCreatedAmqpListener {
    private static final Logger log = LoggerFactory.getLogger(EventCreatedAmqpListener.class);

    private final Mediator mediator;
    private final ObjectMapper objectMapper;

    public EventCreatedAmqpListener(Mediator mediator, ObjectMapper objectMapper) {
        this.mediator = mediator;
        this.objectMapper = objectMapper;
    }

    public void onEventCreatedMessage(String jsonPayload) {
        try {
            log.info("Received event lifecycle message from subscription 'ticketing-seat-generator'");
            EventCreatedPayload payload = objectMapper.readValue(jsonPayload, EventCreatedPayload.class);
            consumeEvent(payload);
        } catch (Exception e) {
            log.error("Failed to parse and process EventCreated message: {}", jsonPayload, e);
            throw new RuntimeException("Error processing EventCreated message: " + e.getMessage(), e);
        }
    }

    public void consumeEvent(EventCreatedPayload payload) {
        log.info("Processing seat generation for created event: {} at venue {}", payload.eventId(), payload.venueId());

        List<SectorDefinition> sectors = payload.sectors().stream()
                .map(s -> new SectorDefinition(
                        s.sectorId(),
                        s.name(),
                        s.type(),
                        s.totalCapacity(),
                        s.basePrice(),
                        s.serviceFee(),
                        s.currency(),
                        s.seatsPerRow()
                ))
                .toList();

        BatchGenerateSeatsCommand command = new BatchGenerateSeatsCommand(
                payload.eventId(),
                payload.venueId(),
                sectors
        );

        Integer generatedCount = mediator.dispatch(command);
        log.info("Successfully generated {} seats for event {}", generatedCount, payload.eventId());
    }
}
