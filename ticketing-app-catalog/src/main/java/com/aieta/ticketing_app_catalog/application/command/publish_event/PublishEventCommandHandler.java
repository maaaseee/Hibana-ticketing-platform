package com.aieta.ticketing_app_catalog.application.command.publish_event;

import com.aieta.ticketing_app_catalog.common.mediator.RequestHandler;
import com.aieta.ticketing_app_catalog.domain.entity.Event;
import com.aieta.ticketing_app_catalog.domain.event.EventPublishedEvent;
import com.aieta.ticketing_app_catalog.domain.exceptions.EventNotFoundException;
import com.aieta.ticketing_app_catalog.domain.port.EventWriteRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class PublishEventCommandHandler implements RequestHandler<PublishEventCommand, Void> {

    private final EventWriteRepositoryPort repository;

    public PublishEventCommandHandler(EventWriteRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Void handle(PublishEventCommand command) {
        Event event = repository.findByIdForUpdate(command.eventId())
                .orElseThrow(() -> new EventNotFoundException(command.eventId()));

        event.publish();

        EventPublishedEvent publishedEvent = new EventPublishedEvent(
                event.getId(),
                Instant.now()
        );

        repository.saveWithOutbox(event, publishedEvent);
        return null;
    }

    @Override
    public Class<PublishEventCommand> getRequestType() {
        return PublishEventCommand.class;
    }
}
