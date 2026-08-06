package com.manekpay.wealth.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TradeResponse(UUID tradeId, String assetSymbol, BigDecimal amount, BigDecimal shares, BigDecimal pricePerShare, Instant createdAt) {
}
