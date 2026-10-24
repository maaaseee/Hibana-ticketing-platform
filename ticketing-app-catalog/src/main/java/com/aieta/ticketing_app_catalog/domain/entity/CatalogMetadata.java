package com.aieta.ticketing_app_catalog.domain.entity;

import java.time.Instant;
import java.util.List;

public record CatalogMetadata(
        Instant furthestEventDate,
        List<VenueSummary> venues
) {}
