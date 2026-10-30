package com.aieta.ticketing_app_orders.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TopUpWalletRequest(
        @NotNull(message = "amount is required")
        @DecimalMin(value = "1.00", message = "Minimum top-up amount is 1.00")
        BigDecimal amount
) {
}
