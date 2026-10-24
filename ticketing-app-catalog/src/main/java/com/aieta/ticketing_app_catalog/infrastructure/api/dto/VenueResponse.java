package com.aieta.ticketing_app_catalog.infrastructure.api.dto;

import java.math.BigDecimal;

public record VenueResponse(
        String id,
        String name,
        String city,
        String country,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String seatMapUrl
) {}
