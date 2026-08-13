package com.manekpay.vaults.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vlt01_vaults")
public class Vault {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    // NULL = the default round-up vault (one per customer, enforced by a partial unique index).
    // Non-null = a named goal vault (unique per customer, also a partial unique index).
    @Column
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "target_amount")
    private BigDecimal targetAmount;

    @Column(name = "sweep_amount")
    private BigDecimal sweepAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "sweep_frequency")
    private SweepFrequency sweepFrequency;

    @Column(name = "sweep_active", nullable = false)
    private boolean sweepActive = false;

    @Column(name = "next_sweep_at")
    private Instant nextSweepAt;

    @Column(name = "last_sweep_at")
    private Instant lastSweepAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Vault() {
    }

    // Default round-up vault - unchanged shape from before this feature.
    public Vault(UUID customerId, Currency currency) {
        this.customerId = customerId;
        this.currency = currency;
    }

    // Named goal vault, funded only by its own recurring sweep (round-ups never touch it).
    public Vault(UUID customerId, String name, Currency currency, BigDecimal targetAmount,
                 BigDecimal sweepAmount, SweepFrequency sweepFrequency) {
        this.customerId = customerId;
        this.name = name;
        this.currency = currency;
        this.targetAmount = targetAmount;
        this.sweepAmount = sweepAmount;
        this.sweepFrequency = sweepFrequency;
        this.sweepActive = true;
        this.nextSweepAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
        this.updatedAt = Instant.now();
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public BigDecimal getSweepAmount() {
        return sweepAmount;
    }

    public void setSweepAmount(BigDecimal sweepAmount) {
        this.sweepAmount = sweepAmount;
        this.updatedAt = Instant.now();
    }

    public SweepFrequency getSweepFrequency() {
        return sweepFrequency;
    }

    public void setSweepFrequency(SweepFrequency sweepFrequency) {
        this.sweepFrequency = sweepFrequency;
        this.updatedAt = Instant.now();
    }

    public boolean isSweepActive() {
        return sweepActive;
    }

    public void setSweepActive(boolean sweepActive) {
        this.sweepActive = sweepActive;
        this.updatedAt = Instant.now();
    }

    public Instant getNextSweepAt() {
        return nextSweepAt;
    }

    public void setNextSweepAt(Instant nextSweepAt) {
        this.nextSweepAt = nextSweepAt;
        this.updatedAt = Instant.now();
    }

    public Instant getLastSweepAt() {
        return lastSweepAt;
    }

    public void setLastSweepAt(Instant lastSweepAt) {
        this.lastSweepAt = lastSweepAt;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
