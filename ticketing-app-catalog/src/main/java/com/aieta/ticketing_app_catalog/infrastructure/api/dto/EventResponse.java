package com.aieta.ticketing_app_catalog.infrastructure.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record EventResponse(
        String id,
        String slug,
        String title,
        String status,
        String category,
        List<String> genres,
        Instant doorsOpenAt,
        Instant startsAt,
        Instant endsAt,
        String timezone,
        String venueName,
        String venueCity,
        String venueAddress,
        String seatMapUrl,
        BigDecimal minPrice,
        String currency,
        String bannerUrl,
        String posterUrl,
        String description,
        String salesPolicyJson,
        List<SectorResponse> sectors
) {}
