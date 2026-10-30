package com.aieta.ticketing_app_orders.infrastructure.persistence.adapter;

import com.aieta.ticketing_app_orders.domain.model.VirtualWallet;
import com.aieta.ticketing_app_orders.domain.ports.WalletRepositoryPort;
import com.aieta.ticketing_app_orders.infrastructure.persistence.entity.VirtualWalletJpaEntity;
import com.aieta.ticketing_app_orders.infrastructure.persistence.mapper.WalletMapper;
import com.aieta.ticketing_app_orders.infrastructure.persistence.repository.SpringDataWalletRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaWalletRepositoryAdapter implements WalletRepositoryPort {

    private final SpringDataWalletRepository springDataWalletRepository;

    public JpaWalletRepositoryAdapter(SpringDataWalletRepository springDataWalletRepository) {
        this.springDataWalletRepository = springDataWalletRepository;
    }

    @Override
    public VirtualWallet save(VirtualWallet wallet) {
        VirtualWalletJpaEntity entity = WalletMapper.toEntity(wallet);
        VirtualWalletJpaEntity saved = springDataWalletRepository.save(entity);
        return WalletMapper.toDomain(saved);
    }

    @Override
    public Optional<VirtualWallet> findByUserId(String userId) {
        return springDataWalletRepository.findByUserId(userId)
                .map(WalletMapper::toDomain);
    }

    @Override
    public Optional<VirtualWallet> findByUserIdForUpdate(String userId) {
        return springDataWalletRepository.findByUserIdForUpdate(userId)
                .map(WalletMapper::toDomain);
    }
}
