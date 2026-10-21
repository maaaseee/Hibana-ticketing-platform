package com.aieta.ticketing_service.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmReservationRequest(
        @NotBlank(message = "orderId is required")
        String orderId
) {
    public ConfirmReservationRequest() {
        this("");
    }
}
