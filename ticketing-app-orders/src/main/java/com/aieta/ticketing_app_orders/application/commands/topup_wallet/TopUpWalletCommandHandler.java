package com.aieta.ticketing_app_orders.application.commands.topup_wallet;

import com.aieta.ticketing_app_orders.application.common.mediator.RequestHandler;
import com.aieta.ticketing_app_orders.application.queries.get_wallet_balance.WalletBalanceDto;
import com.aieta.ticketing_app_orders.domain.model.VirtualWallet;
import com.aieta.ticketing_app_orders.domain.model.pocket.DoubleEntryTransaction;
import com.aieta.ticketing_app_orders.domain.model.pocket.LedgerAccount;
import com.aieta.ticketing_app_orders.domain.model.pocket.LedgerEntry;
import com.aieta.ticketing_app_orders.domain.ports.LedgerRepositoryPort;
import com.aieta.ticketing_app_orders.domain.ports.WalletRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Component
public class TopUpWalletCommandHandler implements RequestHandler<TopUpWalletCommand, WalletBalanceDto> {
    private static final Logger log = LoggerFactory.getLogger(TopUpWalletCommandHandler.class);

    private final WalletRepositoryPort walletRepository;
    private final LedgerRepositoryPort ledgerRepository;

    public TopUpWalletCommandHandler(WalletRepositoryPort walletRepository, LedgerRepositoryPort ledgerRepository) {
        this.walletRepository = walletRepository;
        this.ledgerRepository = ledgerRepository;
    }

    @Override
    @Transactional
    public WalletBalanceDto handle(TopUpWalletCommand request) {
        VirtualWallet wallet = walletRepository.findByUserIdForUpdate(request.userId())
                .orElseGet(() -> VirtualWallet.create(request.userId(), Currency.getInstance("EUR")));

        wallet.credit(request.amount());
        VirtualWallet saved = walletRepository.save(wallet);

        // Record double entry in pocket ledger
        UUID txRef = UUID.randomUUID();
        LedgerAccount assetAccount = ledgerRepository.findAccountByOwnerIdAndType("PLATFORM", LedgerAccount.AccountType.ASSET)
                .orElseGet(() -> ledgerRepository.saveAccount(new LedgerAccount(
                        UUID.randomUUID(), "PLATFORM", LedgerAccount.AccountType.ASSET, request.amount(), Currency.getInstance("EUR"))));

        LedgerAccount liabilityAccount = ledgerRepository.findAccountByOwnerIdAndType("PLATFORM", LedgerAccount.AccountType.LIABILITY)
                .orElseGet(() -> ledgerRepository.saveAccount(new LedgerAccount(
                        UUID.randomUUID(), "PLATFORM", LedgerAccount.AccountType.LIABILITY, request.amount(), Currency.getInstance("EUR"))));

        DoubleEntryTransaction doubleEntryTx = new DoubleEntryTransaction(txRef)
                .addDebit(assetAccount.id(), request.amount(), "Top up bank transfer deposit for user " + request.userId())
                .addCredit(liabilityAccount.id(), request.amount(), "User wallet liability claim for user " + request.userId());

        List<LedgerEntry> entries = doubleEntryTx.buildAndValidate();
        ledgerRepository.recordEntries(entries);

        log.info("Wallet for user {} credited with {} EUR. New balance: {}",
                request.userId(), request.amount(), saved.getBalance());

        return new WalletBalanceDto(
                saved.getUserId(),
                saved.getBalance(),
                saved.getCurrency().getCurrencyCode(),
                saved.getUpdatedAt()
        );
    }

    @Override
    public Class<TopUpWalletCommand> getRequestType() {
        return TopUpWalletCommand.class;
    }
}
