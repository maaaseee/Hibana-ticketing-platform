package com.aieta.ticketing_app_catalog.infrastructure.api.dto;

import java.util.List;

public record HomeHighlightsResponse(
        List<EventResponse> recentlyAdded,
        List<EventResponse> upcoming
) {}
