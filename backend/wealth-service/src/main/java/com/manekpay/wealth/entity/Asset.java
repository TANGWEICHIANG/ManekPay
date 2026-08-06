package com.manekpay.wealth.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wealth01_assets")
public class Asset {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String symbol;

    @Column(nullable = false)
    private String name;

    @Column(name = "price_per_share", nullable = false)
    private BigDecimal pricePerShare;

    @Column(name = "is_shariah_compliant", nullable = false)
    private boolean shariahCompliant;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Asset() {
    }

    public UUID getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPricePerShare() {
        return pricePerShare;
    }

    public boolean isShariahCompliant() {
        return shariahCompliant;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
