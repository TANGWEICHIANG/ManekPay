package com.manekpay.ledger.service;

import com.manekpay.ledger.entity.Currency;

import java.math.BigDecimal;

public interface FxRateProvider {
    BigDecimal getRate(Currency from, Currency to, String bearerToken);
}
