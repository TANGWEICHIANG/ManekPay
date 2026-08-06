package com.manekpay.fx.dto;

import com.manekpay.fx.entity.Currency;

import java.math.BigDecimal;

public record FxRateResponse(Currency from, Currency to, BigDecimal rate) {
}
