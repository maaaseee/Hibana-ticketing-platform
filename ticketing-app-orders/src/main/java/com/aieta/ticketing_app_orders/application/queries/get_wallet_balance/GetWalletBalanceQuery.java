package com.aieta.ticketing_app_orders.application.queries.get_wallet_balance;

import com.aieta.ticketing_app_orders.application.common.mediator.Request;

public record GetWalletBalanceQuery(
        String userId
) implements Request<WalletBalanceDto> {
}
