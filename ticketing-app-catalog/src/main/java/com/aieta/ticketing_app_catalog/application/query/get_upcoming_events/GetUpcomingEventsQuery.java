package com.aieta.ticketing_app_catalog.application.query.get_upcoming_events;

import com.aieta.ticketing_app_catalog.common.mediator.Request;
import com.aieta.ticketing_app_catalog.domain.entity.EventSummary;

import java.util.List;

public record GetUpcomingEventsQuery(String category) implements Request<List<EventSummary>> {
}
