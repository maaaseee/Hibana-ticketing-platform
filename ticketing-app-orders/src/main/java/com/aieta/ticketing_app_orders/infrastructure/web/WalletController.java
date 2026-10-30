package com.aieta.ticketing_app_orders.infrastructure.web;

import com.aieta.ticketing_app_orders.application.commands.topup_wallet.TopUpWalletCommand;
import com.aieta.ticketing_app_orders.application.common.mediator.Mediator;
import com.aieta.ticketing_app_orders.application.queries.get_wallet_balance.GetWalletBalanceQuery;
import com.aieta.ticketing_app_orders.application.queries.get_wallet_balance.WalletBalanceDto;
import com.aieta.ticketing_app_orders.infrastructure.web.dto.TopUpWalletRequest;
import com.aieta.ticketing_app_orders.infrastructure.web.dto.WalletBalanceResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private final Mediator mediator;

    public WalletController(Mediator mediator) {
        this.mediator = mediator;
    }

    @GetMapping("/me/balance")
    public WalletBalanceResponse getBalance(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {
        String effectiveUserId = (headerUserId != null && !headerUserId.isBlank()) ? headerUserId : "anonymous-user";
        WalletBalanceDto dto = mediator.dispatch(new GetWalletBalanceQuery(effectiveUserId));
        return new WalletBalanceResponse(dto.userId(), dto.balance(), dto.currency(), dto.updatedAt());
    }

    @PostMapping("/me/top-up")
    public WalletBalanceResponse topUp(
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @Valid @RequestBody TopUpWalletRequest request) {
        String effectiveUserId = (headerUserId != null && !headerUserId.isBlank()) ? headerUserId : "anonymous-user";
        WalletBalanceDto dto = mediator.dispatch(new TopUpWalletCommand(effectiveUserId, request.amount()));
        return new WalletBalanceResponse(dto.userId(), dto.balance(), dto.currency(), dto.updatedAt());
    }
}
