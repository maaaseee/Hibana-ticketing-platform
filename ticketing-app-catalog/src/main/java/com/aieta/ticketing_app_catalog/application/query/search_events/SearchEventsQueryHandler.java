package com.aieta.ticketing_app_catalog.application.query.search_events;

import com.aieta.ticketing_app_catalog.common.mediator.RequestHandler;
import com.aieta.ticketing_app_catalog.domain.entity.EventSummary;
import com.aieta.ticketing_app_catalog.domain.port.EventRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SearchEventsQueryHandler implements RequestHandler<SearchEventsQuery, List<EventSummary>> {

    private final EventRepositoryPort eventRepositoryPort;

    public SearchEventsQueryHandler(EventRepositoryPort eventRepositoryPort) {
        this.eventRepositoryPort = eventRepositoryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSummary> handle(SearchEventsQuery query) {
        String q = query.query() != null ? query.query().trim() : null;
        String v = query.venue() != null ? query.venue().trim() : null;
        return eventRepositoryPort.searchEventsFuzzy(q, v);
    }

    @Override
    public Class<SearchEventsQuery> getRequestType() {
        return SearchEventsQuery.class;
    }
}
