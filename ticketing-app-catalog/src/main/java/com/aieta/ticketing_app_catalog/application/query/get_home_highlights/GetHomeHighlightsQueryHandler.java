package com.aieta.ticketing_app_catalog.application.query.get_home_highlights;

import com.aieta.ticketing_app_catalog.common.mediator.RequestHandler;
import com.aieta.ticketing_app_catalog.domain.entity.EventSummary;
import com.aieta.ticketing_app_catalog.domain.entity.HomeHighlights;
import com.aieta.ticketing_app_catalog.domain.port.EventRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class GetHomeHighlightsQueryHandler implements RequestHandler<GetHomeHighlightsQuery, HomeHighlights> {

    private final EventRepositoryPort eventRepositoryPort;

    public GetHomeHighlightsQueryHandler(EventRepositoryPort eventRepositoryPort) {
        this.eventRepositoryPort = eventRepositoryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public HomeHighlights handle(GetHomeHighlightsQuery query) {
        List<EventSummary> recentlyAdded = eventRepositoryPort.findRecentlyAddedPublished();
        Instant ref = query.referenceDate() != null ? query.referenceDate() : Instant.now();
        List<EventSummary> upcoming = eventRepositoryPort.findUpcomingPublishedTop3(ref);

        return new HomeHighlights(recentlyAdded, upcoming);
    }

    @Override
    public Class<GetHomeHighlightsQuery> getRequestType() {
        return GetHomeHighlightsQuery.class;
    }
}
