package com.aieta.ticketing_app_orders.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateOrderRequest(
        @NotBlank(message = "reservationId is required")
        String reservationId,

        @NotBlank(message = "eventId is required")
        String eventId,

        @NotBlank(message = "seatId is required")
        String seatId,

        @NotNull(message = "totalAmount is required")
        @DecimalMin(value = "0.00", message = "totalAmount cannot be negative")
        BigDecimal totalAmount,

        BigDecimal subtotalAmount,
        BigDecimal serviceFeeAmount
) {
    public CreateOrderRequest(String reservationId, String eventId, String seatId, BigDecimal totalAmount) {
        this(reservationId, eventId, seatId, totalAmount, null, null);
    }
}
