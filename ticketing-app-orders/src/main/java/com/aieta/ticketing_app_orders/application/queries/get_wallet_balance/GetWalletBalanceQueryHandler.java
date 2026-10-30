package com.aieta.ticketing_app_orders.application.queries.get_wallet_balance;

import com.aieta.ticketing_app_orders.application.common.mediator.RequestHandler;
import com.aieta.ticketing_app_orders.domain.model.VirtualWallet;
import com.aieta.ticketing_app_orders.domain.ports.WalletRepositoryPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

@Component
public class GetWalletBalanceQueryHandler implements RequestHandler<GetWalletBalanceQuery, WalletBalanceDto> {

    private final WalletRepositoryPort walletRepository;

    public GetWalletBalanceQueryHandler(WalletRepositoryPort walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    public WalletBalanceDto handle(GetWalletBalanceQuery request) {
        return walletRepository.findByUserId(request.userId())
                .map(w -> new WalletBalanceDto(w.getUserId(), w.getBalance(), w.getCurrency().getCurrencyCode(), w.getUpdatedAt()))
                .orElseGet(() -> new WalletBalanceDto(request.userId(), BigDecimal.ZERO, "EUR", Instant.now()));
    }

    @Override
    public Class<GetWalletBalanceQuery> getRequestType() {
        return GetWalletBalanceQuery.class;
    }
}
