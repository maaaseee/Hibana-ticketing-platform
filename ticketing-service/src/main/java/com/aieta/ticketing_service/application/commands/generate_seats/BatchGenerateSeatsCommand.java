package com.aieta.ticketing_service.application.commands.generate_seats;

import com.aieta.ticketing_service.application.common.mediator.Request;

import java.util.List;
import java.util.Objects;

public record BatchGenerateSeatsCommand(
        String eventId,
        String venueId,
        List<SectorDefinition> sectors
) implements Request<Integer> {
    public BatchGenerateSeatsCommand {
        Objects.requireNonNull(eventId, "eventId cannot be null");
        Objects.requireNonNull(venueId, "venueId cannot be null");
        Objects.requireNonNull(sectors, "sectors cannot be null");
    }
}
