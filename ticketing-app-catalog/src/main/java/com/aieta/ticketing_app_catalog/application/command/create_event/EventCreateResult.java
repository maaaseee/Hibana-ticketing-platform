package com.aieta.ticketing_app_catalog.application.command.create_event;

import com.aieta.ticketing_app_catalog.domain.entity.EventStatus;

import java.time.Instant;

public record EventCreateResult(
        String id,
        String slug,
        EventStatus status,
        Instant createdAt,
        int totalEstimatedCapacity
) {}
