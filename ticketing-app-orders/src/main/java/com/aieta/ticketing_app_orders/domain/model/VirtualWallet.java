package com.aieta.ticketing_app_orders.domain.model;

import com.aieta.ticketing_app_orders.domain.exceptions.DomainException;
import com.aieta.ticketing_app_orders.domain.exceptions.InsufficientFundsException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

public class VirtualWallet {
    private final UUID id;
    private final String userId;
    private BigDecimal balance;
    private final Currency currency;
    private final Instant createdAt;
    private Instant updatedAt;

    public VirtualWallet(UUID id, String userId, BigDecimal balance, Currency currency, Instant createdAt, Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.userId = Objects.requireNonNull(userId, "userId cannot be null");
        this.balance = balance != null ? balance : BigDecimal.ZERO;
        this.currency = currency != null ? currency : Currency.getInstance("EUR");
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
        if (this.balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainException("Wallet balance cannot be negative");
        }
    }

    public static VirtualWallet create(String userId, Currency currency) {
        return new VirtualWallet(UUID.randomUUID(), userId, BigDecimal.ZERO, currency, Instant.now(), Instant.now());
    }

    public void credit(BigDecimal amount) {
        Objects.requireNonNull(amount, "Credit amount cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Credit amount must be positive");
        }
        this.balance = this.balance.add(amount);
        this.updatedAt = Instant.now();
    }

    public void debit(BigDecimal amount) {
        Objects.requireNonNull(amount, "Debit amount cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Debit amount must be positive");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(this.balance, amount);
        }
        this.balance = this.balance.subtract(amount);
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Currency getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
