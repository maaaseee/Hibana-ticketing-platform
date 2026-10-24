package com.aieta.ticketing_app_catalog.application.query.get_event_by_id;

import com.aieta.ticketing_app_catalog.common.mediator.RequestHandler;
import com.aieta.ticketing_app_catalog.domain.entity.EventSummary;
import com.aieta.ticketing_app_catalog.domain.exceptions.EventNotFoundException;
import com.aieta.ticketing_app_catalog.domain.exceptions.InvalidEventIdentifierException;
import com.aieta.ticketing_app_catalog.domain.port.EventRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetEventByIdQueryHandler implements RequestHandler<GetEventByIdQuery, EventSummary> {

    private final EventRepositoryPort eventRepositoryPort;

    public GetEventByIdQueryHandler(EventRepositoryPort eventRepositoryPort) {
        this.eventRepositoryPort = eventRepositoryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public EventSummary handle(GetEventByIdQuery query) {
        if (query.eventId() == null || query.eventId().isBlank()) {
            throw new InvalidEventIdentifierException("Event ID cannot be null or blank");
        }
        String idOrSlug = query.eventId().trim();
        return eventRepositoryPort.findById(idOrSlug)
                .or(() -> eventRepositoryPort.findBySlug(idOrSlug))
                .orElseThrow(() -> new EventNotFoundException(query.eventId()));
    }

    @Override
    public Class<GetEventByIdQuery> getRequestType() {
        return GetEventByIdQuery.class;
    }
}
