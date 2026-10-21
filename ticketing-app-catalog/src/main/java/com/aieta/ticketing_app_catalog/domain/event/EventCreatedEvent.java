package com.aieta.ticketing_app_catalog.domain.event;

import com.aieta.ticketing_app_catalog.domain.entity.EventSectorSummary;

import java.time.Instant;
import java.util.List;

public record EventCreatedEvent(
        String eventId,
        String organizerId,
        String venueId,
        String slug,
        String title,
        Instant startsAt,
        List<EventSectorSummary> sectors,
        Instant occurredAt
) {}
