package com.aieta.ticketing_app_orders.infrastructure.persistence.mapper;

import com.aieta.ticketing_app_orders.domain.model.VirtualWallet;
import com.aieta.ticketing_app_orders.infrastructure.persistence.entity.VirtualWalletJpaEntity;

import java.util.Currency;

public final class WalletMapper {

    private WalletMapper() {
    }

    public static VirtualWallet toDomain(VirtualWalletJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new VirtualWallet(
                entity.getId(),
                entity.getUserId(),
                entity.getBalance(),
                Currency.getInstance(entity.getCurrency()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static VirtualWalletJpaEntity toEntity(VirtualWallet domain) {
        if (domain == null) {
            return null;
        }
        return new VirtualWalletJpaEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getBalance(),
                domain.getCurrency().getCurrencyCode(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
