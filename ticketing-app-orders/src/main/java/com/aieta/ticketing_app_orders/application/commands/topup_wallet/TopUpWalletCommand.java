package com.aieta.ticketing_app_orders.application.commands.topup_wallet;

import com.aieta.ticketing_app_orders.application.common.mediator.Request;
import com.aieta.ticketing_app_orders.application.queries.get_wallet_balance.WalletBalanceDto;

import java.math.BigDecimal;

public record TopUpWalletCommand(
        String userId,
        BigDecimal amount
) implements Request<WalletBalanceDto> {
}
