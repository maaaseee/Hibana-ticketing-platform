package com.aieta.ticketing_app_catalog.domain.event;

import java.time.Instant;

public record EventCancelledEvent(
        String eventId,
        String cancellationReason,
        Instant occurredAt
) {}
