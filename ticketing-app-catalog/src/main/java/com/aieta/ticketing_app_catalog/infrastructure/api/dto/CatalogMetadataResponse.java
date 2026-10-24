package com.aieta.ticketing_app_catalog.infrastructure.api.dto;

import java.time.Instant;
import java.util.List;

public record CatalogMetadataResponse(
        Instant furthestEventDate,
        List<VenueResponse> venues
) {}
