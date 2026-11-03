package com.aieta.ticketing_app_catalog.application.query.get_all_events;

import com.aieta.ticketing_app_catalog.common.mediator.Request;
import com.aieta.ticketing_app_catalog.domain.entity.EventSummary;

import java.util.List;

public record GetAllEventsQuery(String status) implements Request<List<EventSummary>> {
}
