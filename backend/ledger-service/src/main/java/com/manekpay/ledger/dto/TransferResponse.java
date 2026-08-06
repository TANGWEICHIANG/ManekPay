package com.manekpay.ledger.dto;

import com.manekpay.ledger.entity.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(UUID transferId, BigDecimal sourceAmount, Currency sourceCurrency,
                                BigDecimal destAmount, Currency destCurrency, BigDecimal fxRate, Instant createdAt) {
}
