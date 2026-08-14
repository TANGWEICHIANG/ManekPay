package com.manekpay.risk.dto;

import java.time.Instant;

public record RiskStatusResponse(boolean restricted, Instant restrictedUntil) {
}
