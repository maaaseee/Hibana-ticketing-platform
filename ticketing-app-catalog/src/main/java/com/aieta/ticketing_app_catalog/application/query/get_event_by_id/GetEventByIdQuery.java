package com.aieta.ticketing_app_catalog.application.query.get_event_by_id;

import com.aieta.ticketing_app_catalog.common.mediator.Request;
import com.aieta.ticketing_app_catalog.domain.entity.EventSummary;

public record GetEventByIdQuery(String eventId) implements Request<EventSummary> {
}
