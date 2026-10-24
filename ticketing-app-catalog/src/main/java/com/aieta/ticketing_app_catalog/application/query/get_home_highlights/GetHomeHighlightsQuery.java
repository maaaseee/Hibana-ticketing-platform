package com.aieta.ticketing_app_catalog.application.query.get_home_highlights;

import com.aieta.ticketing_app_catalog.common.mediator.Request;
import com.aieta.ticketing_app_catalog.domain.entity.HomeHighlights;

import java.time.Instant;

public record GetHomeHighlightsQuery(Instant referenceDate) implements Request<HomeHighlights> {
}
