package com.aieta.ticketing_app_catalog.infrastructure.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record CreateEventRequest(
        @NotBlank(message = "Organizer ID is required")
        String organizerId,

        @NotBlank(message = "Venue ID is required")
        String venueId,

        @NotBlank(message = "Event slug is required")
        String slug,

        @NotBlank(message = "Event title is required")
        String title,

        String category,
        List<String> genres,

        @NotNull(message = "Doors open time is required")
        Instant doorsOpenAt,

        @NotNull(message = "Starts at time is required")
        Instant startsAt,

        @NotNull(message = "Ends at time is required")
        Instant endsAt,

        String timezone,
        String media,
        String salesPolicy,

        @NotEmpty(message = "At least one sector must be provided")
        @Valid
        List<CreateEventSectorRequest> sectors
) {}
