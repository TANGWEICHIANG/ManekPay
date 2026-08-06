package com.manekpay.risk.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "risk01_flags")
public class RiskFlag {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(nullable = false)
    private String rule;

    @Column(nullable = false)
    private String detail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected RiskFlag() {
    }

    public RiskFlag(UUID customerId, UUID transactionId, String rule, String detail) {
        this.customerId = customerId;
        this.transactionId = transactionId;
        this.rule = rule;
        this.detail = detail;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public String getRule() {
        return rule;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
