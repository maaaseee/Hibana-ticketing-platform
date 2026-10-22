package com.aieta.ticketing_app_inventory.application.queries.get_event_seats;

import com.aieta.ticketing_app_inventory.application.common.mediator.Request;

import java.util.Objects;
import java.util.Optional;

public record GetEventSeatsQuery(
        String eventId,
        Optional<String> sectorId
) implements Request<EventSeatsResponse> {
    public GetEventSeatsQuery {
        Objects.requireNonNull(eventId, "eventId cannot be null");
        if (sectorId == null) {
            sectorId = Optional.empty();
        }
    }

    public GetEventSeatsQuery(String eventId) {
        this(eventId, Optional.empty());
    }

    public GetEventSeatsQuery(String eventId, String sectorId) {
        this(eventId, Optional.ofNullable(sectorId));
    }
}
