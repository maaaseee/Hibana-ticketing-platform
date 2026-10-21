package com.aieta.ticketing_app_catalog.domain.entity;

import com.aieta.ticketing_app_catalog.domain.exceptions.InvalidEventScheduleException;
import com.aieta.ticketing_app_catalog.domain.exceptions.InvalidEventTitleException;

import java.time.Instant;
import java.util.List;

public record EventSummary(
        String id,
        String slug,
        String title,
        EventStatus status,
        String category,
        List<String> genres,
        Instant doorsOpenAt,
        Instant startsAt,
        Instant endsAt,
        String timezone,
        String mediaJson,
        String salesPolicyJson,
        VenueSummary venue,
        List<EventSectorSummary> sectors
) {
    public EventSummary {
        if (title == null || title.isBlank()) {
            throw new InvalidEventTitleException("Event title cannot be null or blank");
        }
        if (doorsOpenAt != null && startsAt != null && doorsOpenAt.isAfter(startsAt)) {
            throw InvalidEventScheduleException.doorsAfterStart(doorsOpenAt, startsAt);
        }
        if (startsAt != null && endsAt != null && startsAt.isAfter(endsAt)) {
            throw InvalidEventScheduleException.endBeforeStart(startsAt, endsAt);
        }
    }
}
