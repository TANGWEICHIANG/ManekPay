package com.manekpay.vaults.dto;

import com.manekpay.vaults.entity.Currency;
import com.manekpay.vaults.entity.SweepFrequency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record GoalResponse(UUID id, String name, Currency currency, BigDecimal balance, BigDecimal targetAmount,
                            BigDecimal sweepAmount, SweepFrequency sweepFrequency, boolean sweepActive,
                            Instant nextSweepAt, Instant lastSweepAt) {
}
