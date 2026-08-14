package com.manekpay.ledger.dto;

import com.manekpay.ledger.entity.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// fxRate converts sourceAmount -> destAmount (multiply: destAmount = sourceAmount * fxRate).
// topUpFxRate converts topUpAmount -> the source-currency shortfall it funded (multiply:
// shortfallInSourceCurrency = topUpAmount * topUpFxRate) - i.e. topUpCurrency -> sourceCurrency,
// the OPPOSITE direction from fxRate. A consumer must not assume both rates share one convention.
public record TransferResponse(UUID transferId, BigDecimal sourceAmount, Currency sourceCurrency,
                                BigDecimal destAmount, Currency destCurrency, BigDecimal fxRate, Instant createdAt,
                                BigDecimal topUpAmount, Currency topUpCurrency, BigDecimal topUpFxRate) {
}
