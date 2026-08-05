package com.manekpay.ledger.dto;

import com.manekpay.ledger.entity.ProxyType;

import java.time.Instant;
import java.util.UUID;

public record ProxyResponse(UUID proxyId, ProxyType type, String value, Instant createdAt) {
}
