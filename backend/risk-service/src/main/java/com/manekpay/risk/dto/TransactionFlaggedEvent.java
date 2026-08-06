package com.manekpay.risk.dto;

import java.time.Instant;
import java.util.UUID;

public record TransactionFlaggedEvent(UUID transactionId, UUID customerId, String rule, String detail, Instant occurredAt) {
}
