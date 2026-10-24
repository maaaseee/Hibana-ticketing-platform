package com.aieta.ticketing_app_catalog.application.query.search_events;

import com.aieta.ticketing_app_catalog.common.mediator.Request;
import com.aieta.ticketing_app_catalog.domain.entity.EventSummary;

import java.util.List;

public record SearchEventsQuery(String query, String venue) implements Request<List<EventSummary>> {
}
