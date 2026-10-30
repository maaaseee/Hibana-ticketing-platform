package com.aieta.ticketing_app_orders.infrastructure.persistence.mapper;

import com.aieta.ticketing_app_orders.domain.model.pocket.LedgerAccount;
import com.aieta.ticketing_app_orders.domain.model.pocket.LedgerEntry;
import com.aieta.ticketing_app_orders.infrastructure.persistence.entity.LedgerAccountJpaEntity;
import com.aieta.ticketing_app_orders.infrastructure.persistence.entity.LedgerEntryJpaEntity;

import java.util.Currency;

public final class LedgerMapper {

    private LedgerMapper() {
    }

    public static LedgerAccount toDomainAccount(LedgerAccountJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new LedgerAccount(
                entity.getId(),
                entity.getOwnerId(),
                LedgerAccount.AccountType.valueOf(entity.getAccountType()),
                entity.getBalance(),
                Currency.getInstance(entity.getCurrency())
        );
    }

    public static LedgerAccountJpaEntity toEntityAccount(LedgerAccount domain) {
        if (domain == null) {
            return null;
        }
        return new LedgerAccountJpaEntity(
                domain.id(),
                domain.ownerId(),
                domain.type().name(),
                domain.balance(),
                domain.currency().getCurrencyCode(),
                null
        );
    }

    public static LedgerEntryJpaEntity toEntityEntry(LedgerEntry domain) {
        if (domain == null) {
            return null;
        }
        return new LedgerEntryJpaEntity(
                domain.id(),
                domain.accountId(),
                domain.entryType().name(),
                domain.amount(),
                domain.transactionRef(),
                domain.description(),
                domain.postedAt()
        );
    }
}
