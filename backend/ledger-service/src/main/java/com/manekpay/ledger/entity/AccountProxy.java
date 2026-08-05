package com.manekpay.ledger.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ldg03_account_proxies")
public class AccountProxy {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProxyType type;

    @Column(nullable = false)
    private String value;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AccountProxy() {
    }

    public AccountProxy(UUID accountId, ProxyType type, String value) {
        this.accountId = accountId;
        this.type = type;
        this.value = value;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public ProxyType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
