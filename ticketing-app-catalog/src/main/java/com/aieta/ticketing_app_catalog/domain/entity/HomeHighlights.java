package com.aieta.ticketing_app_catalog.domain.entity;

import java.util.List;

public record HomeHighlights(
        List<EventSummary> recentlyAdded,
        List<EventSummary> upcoming
) {}
