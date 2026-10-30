package com.aieta.ticketing_app_orders.domain.ports;

import com.aieta.ticketing_app_orders.domain.model.VirtualWallet;

import java.util.Optional;

public interface WalletRepositoryPort {
    VirtualWallet save(VirtualWallet wallet);
    Optional<VirtualWallet> findByUserId(String userId);
    Optional<VirtualWallet> findByUserIdForUpdate(String userId);
}
