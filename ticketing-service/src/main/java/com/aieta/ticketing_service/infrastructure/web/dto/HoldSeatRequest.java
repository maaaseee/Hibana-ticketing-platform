package com.aieta.ticketing_service.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record HoldSeatRequest(
        @NotBlank(message = "eventId is required")
        String eventId,

        @NotBlank(message = "seatId is required")
        String seatId,

        @NotBlank(message = "userId is required")
        String userId
) {
}
