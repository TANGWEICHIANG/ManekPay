package com.manekpay.risk.dto;

import java.time.Instant;
import java.util.UUID;

public record FlagResponse(UUID flagId, UUID transactionId, String rule, String detail, Instant createdAt) {
}
