package com.aieta.ticketing_app_orders.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record WalletBalanceResponse(
        String userId,
        BigDecimal balance,
        String currency,
        Instant updatedAt
) {
}
