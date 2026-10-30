package com.aieta.ticketing_app_orders.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries", schema = "orders")
public class LedgerEntryJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "entry_type", length = 6, nullable = false)
    private String entryType;

    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "transaction_ref", nullable = false)
    private UUID transactionRef;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "posted_at", nullable = false, updatable = false)
    private Instant postedAt = Instant.now();

    public LedgerEntryJpaEntity() {
    }

    public LedgerEntryJpaEntity(UUID id, UUID accountId, String entryType, BigDecimal amount, UUID transactionRef, String description, Instant postedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.accountId = accountId;
        this.entryType = entryType;
        this.amount = amount;
        this.transactionRef = transactionRef;
        this.description = description;
        this.postedAt = postedAt != null ? postedAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getEntryType() {
        return entryType;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public UUID getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(UUID transactionRef) {
        this.transactionRef = transactionRef;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(Instant postedAt) {
        this.postedAt = postedAt;
    }
}
