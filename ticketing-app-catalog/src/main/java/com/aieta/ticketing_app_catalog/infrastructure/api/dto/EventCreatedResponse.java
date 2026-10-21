package com.aieta.ticketing_app_catalog.infrastructure.api.dto;

import com.aieta.ticketing_app_catalog.domain.entity.EventStatus;

import java.time.Instant;

public record EventCreatedResponse(
        String id,
        String slug,
        EventStatus status,
        Instant createdAt,
        int estimatedSeatsToGenerate
) {}
