package com.manekpay.ledger.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// Append-only audit record of money leaving the ledger via a vault sweep. Deliberately not a
// Transfer: Transfer requires both a fromWalletId and toWalletId inside this service's own
// account graph, but a sweep's "recipient" is a vault in vaults-service, which has no Wallet row
// here — see docs/superpowers/specs/2026-08-07-goal-vaults-recurring-sweeps-design.md §4.2.
@Entity
@Table(name = "ldg06_vault_sweep_debits")
public class VaultSweepDebit {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    @Column(name = "balance_after", nullable = false)
    private BigDecimal balanceAfter;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected VaultSweepDebit() {
    }

    public VaultSweepDebit(UUID walletId, UUID customerId, BigDecimal amount, Currency currency,
                            BigDecimal balanceAfter, String reference) {
        this.walletId = walletId;
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.balanceAfter = balanceAfter;
        this.reference = reference;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getReference() {
        return reference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
