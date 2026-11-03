package com.aieta.ticketing_app_catalog.application.query.get_all_events;

import com.aieta.ticketing_app_catalog.common.mediator.RequestHandler;
import com.aieta.ticketing_app_catalog.domain.entity.EventSummary;
import com.aieta.ticketing_app_catalog.domain.port.EventRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GetAllEventsQueryHandler implements RequestHandler<GetAllEventsQuery, List<EventSummary>> {

    private final EventRepositoryPort eventRepositoryPort;

    public GetAllEventsQueryHandler(EventRepositoryPort eventRepositoryPort) {
        this.eventRepositoryPort = eventRepositoryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSummary> handle(GetAllEventsQuery query) {
        if (query.status() == null || query.status().isBlank() || query.status().equalsIgnoreCase("ALL")) {
            return eventRepositoryPort.findAllEvents();
        }
        return eventRepositoryPort.findByStatus(query.status().trim().toUpperCase());
    }

    @Override
    public Class<GetAllEventsQuery> getRequestType() {
        return GetAllEventsQuery.class;
    }
}
