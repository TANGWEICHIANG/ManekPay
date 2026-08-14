package com.manekpay.ledger.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ldg04_transfers")
public class Transfer {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "from_wallet_id", nullable = false)
    private UUID fromWalletId;

    @Column(name = "to_wallet_id", nullable = false)
    private UUID toWalletId;

    @Column(name = "source_amount", nullable = false)
    private BigDecimal sourceAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_currency", nullable = false)
    private Currency sourceCurrency;

    @Column(name = "dest_amount", nullable = false)
    private BigDecimal destAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "dest_currency", nullable = false)
    private Currency destCurrency;

    @Column(name = "fx_rate")
    private BigDecimal fxRate;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Column(name = "top_up_amount")
    private BigDecimal topUpAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "top_up_currency")
    private Currency topUpCurrency;

    @Column(name = "top_up_fx_rate")
    private BigDecimal topUpFxRate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Transfer() {
    }

    public Transfer(UUID fromWalletId, UUID toWalletId, BigDecimal sourceAmount, Currency sourceCurrency,
                     BigDecimal destAmount, Currency destCurrency, BigDecimal fxRate, String idempotencyKey,
                     BigDecimal topUpAmount, Currency topUpCurrency, BigDecimal topUpFxRate) {
        this.fromWalletId = fromWalletId;
        this.toWalletId = toWalletId;
        this.sourceAmount = sourceAmount;
        this.sourceCurrency = sourceCurrency;
        this.destAmount = destAmount;
        this.destCurrency = destCurrency;
        this.fxRate = fxRate;
        this.idempotencyKey = idempotencyKey;
        this.topUpAmount = topUpAmount;
        this.topUpCurrency = topUpCurrency;
        this.topUpFxRate = topUpFxRate;
    }

    public UUID getId() {
        return id;
    }

    public UUID getFromWalletId() {
        return fromWalletId;
    }

    public UUID getToWalletId() {
        return toWalletId;
    }

    public BigDecimal getSourceAmount() {
        return sourceAmount;
    }

    public Currency getSourceCurrency() {
        return sourceCurrency;
    }

    public BigDecimal getDestAmount() {
        return destAmount;
    }

    public Currency getDestCurrency() {
        return destCurrency;
    }

    public BigDecimal getFxRate() {
        return fxRate;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public BigDecimal getTopUpAmount() {
        return topUpAmount;
    }

    public Currency getTopUpCurrency() {
        return topUpCurrency;
    }

    public BigDecimal getTopUpFxRate() {
        return topUpFxRate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
