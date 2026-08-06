package com.manekpay.vaults.dto;

import com.manekpay.vaults.entity.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionCreatedEvent(UUID transactionId, UUID customerId, BigDecimal amount, Currency currency, Currency homeCurrency, Instant occurredAt) {
}
