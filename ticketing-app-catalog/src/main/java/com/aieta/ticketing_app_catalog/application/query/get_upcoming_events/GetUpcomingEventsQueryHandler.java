package com.aieta.ticketing_app_catalog.application.query.get_upcoming_events;

import com.aieta.ticketing_app_catalog.common.mediator.RequestHandler;
import com.aieta.ticketing_app_catalog.domain.entity.EventSummary;
import com.aieta.ticketing_app_catalog.domain.port.EventRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GetUpcomingEventsQueryHandler implements RequestHandler<GetUpcomingEventsQuery, List<EventSummary>> {

    private final EventRepositoryPort eventRepositoryPort;

    public GetUpcomingEventsQueryHandler(EventRepositoryPort eventRepositoryPort) {
        this.eventRepositoryPort = eventRepositoryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSummary> handle(GetUpcomingEventsQuery query) {
        if (query.category() != null && !query.category().isBlank()) {
            return eventRepositoryPort.findByCategoryAndPublished(query.category().trim());
        }
        return eventRepositoryPort.findUpcomingPublished();
    }

    @Override
    public Class<GetUpcomingEventsQuery> getRequestType() {
        return GetUpcomingEventsQuery.class;
    }
}
