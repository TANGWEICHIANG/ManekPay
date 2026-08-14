package com.manekpay.ledger.dto;

import java.time.Instant;

public record RiskStatusResponse(boolean restricted, Instant restrictedUntil) {
}
