package com.aieta.ticketing_app_catalog.application.command.create_event;

import com.aieta.ticketing_app_catalog.common.mediator.Request;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CreateEventCommand(
        String organizerId,
        String venueId,
        String slug,
        String title,
        String category,
        List<String> genres,
        Instant doorsOpenAt,
        Instant startsAt,
        Instant endsAt,
        String timezone,
        String media,
        String salesPolicy,
        List<SectorInput> sectors
) implements Request<EventCreateResult> {

    public record SectorInput(
            String name,
            String type,
            int totalCapacity,
            BigDecimal basePrice,
            BigDecimal serviceFee,
            String currency
    ) {}
}
