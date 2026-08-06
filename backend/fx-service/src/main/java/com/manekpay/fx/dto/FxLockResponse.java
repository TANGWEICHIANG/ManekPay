package com.manekpay.fx.dto;

import com.manekpay.fx.entity.Currency;

import java.math.BigDecimal;
import java.time.Instant;

public record FxLockResponse(String lockId, Currency from, Currency to, BigDecimal rate, Instant expiresAt) {
}
