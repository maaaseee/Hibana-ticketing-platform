package com.aieta.ticketing_app_catalog.application.command.cancel_event;

import com.aieta.ticketing_app_catalog.common.mediator.RequestHandler;
import com.aieta.ticketing_app_catalog.domain.entity.Event;
import com.aieta.ticketing_app_catalog.domain.event.EventCancelledEvent;
import com.aieta.ticketing_app_catalog.domain.exceptions.EventNotFoundException;
import com.aieta.ticketing_app_catalog.domain.port.EventWriteRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class CancelEventCommandHandler implements RequestHandler<CancelEventCommand, Void> {

    private final EventWriteRepositoryPort repository;

    public CancelEventCommandHandler(EventWriteRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Void handle(CancelEventCommand command) {
        Event event = repository.findByIdForUpdate(command.eventId())
                .orElseThrow(() -> new EventNotFoundException(command.eventId()));

        event.cancel(command.reason());

        EventCancelledEvent cancelledEvent = new EventCancelledEvent(
                event.getId(),
                command.reason() != null ? command.reason() : "Cancelled by administrator",
                Instant.now()
        );

        repository.saveWithOutbox(event, cancelledEvent);
        return null;
    }

    @Override
    public Class<CancelEventCommand> getRequestType() {
        return CancelEventCommand.class;
    }
}
