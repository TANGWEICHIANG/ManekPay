package com.manekpay.vaults.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vlt02_round_ups")
public class RoundUp {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "vault_id", nullable = false)
    private UUID vaultId;

    @Column(name = "transaction_id", nullable = false, unique = true)
    private UUID transactionId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected RoundUp() {
    }

    public RoundUp(UUID vaultId, UUID transactionId, BigDecimal amount) {
        this.vaultId = vaultId;
        this.transactionId = transactionId;
        this.amount = amount;
    }

    public UUID getId() {
        return id;
    }

    public UUID getVaultId() {
        return vaultId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
