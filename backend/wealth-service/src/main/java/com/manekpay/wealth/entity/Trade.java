package com.manekpay.wealth.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wealth02_trades")
public class Trade {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private BigDecimal shares;

    @Column(name = "price_per_share", nullable = false)
    private BigDecimal pricePerShare;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Trade() {
    }

    public Trade(UUID customerId, UUID assetId, BigDecimal amount, BigDecimal shares, BigDecimal pricePerShare, String idempotencyKey) {
        this.customerId = customerId;
        this.assetId = assetId;
        this.amount = amount;
        this.shares = shares;
        this.pricePerShare = pricePerShare;
        this.idempotencyKey = idempotencyKey;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getAssetId() {
        return assetId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getShares() {
        return shares;
    }

    public BigDecimal getPricePerShare() {
        return pricePerShare;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
