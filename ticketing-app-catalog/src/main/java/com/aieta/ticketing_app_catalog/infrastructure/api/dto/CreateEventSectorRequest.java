package com.aieta.ticketing_app_catalog.infrastructure.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateEventSectorRequest(
        @NotBlank(message = "Sector name is required")
        String name,

        String type,

        @Min(value = 1, message = "Total capacity must be at least 1")
        int totalCapacity,

        @NotNull(message = "Base price is required")
        @DecimalMin(value = "0.0", message = "Base price cannot be negative")
        BigDecimal basePrice,

        @DecimalMin(value = "0.0", message = "Service fee cannot be negative")
        BigDecimal serviceFee,

        String currency
) {}
