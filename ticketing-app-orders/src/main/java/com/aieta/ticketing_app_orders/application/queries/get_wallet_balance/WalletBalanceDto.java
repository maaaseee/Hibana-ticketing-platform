package com.aieta.ticketing_app_orders.application.queries.get_wallet_balance;

import java.math.BigDecimal;
import java.time.Instant;

public record WalletBalanceDto(
        String userId,
        BigDecimal balance,
        String currency,
        Instant updatedAt
) {
}
