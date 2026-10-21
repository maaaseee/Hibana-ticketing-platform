package com.aieta.ticketing_app_catalog.domain.event;

import java.time.Instant;

public record EventPublishedEvent(
        String eventId,
        Instant publishedAt
) {}
