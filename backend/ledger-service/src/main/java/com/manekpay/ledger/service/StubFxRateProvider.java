package com.manekpay.ledger.service;

import com.manekpay.ledger.entity.Currency;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class StubFxRateProvider implements FxRateProvider {

    // ponytail: fixed illustrative rates, not live market data — swap this
    // implementation for a real selat-fx-service client when that service exists.
    private static final Map<String, BigDecimal> RATES = Map.ofEntries(
            Map.entry("MYR-SGD", new BigDecimal("0.30")),
            Map.entry("SGD-MYR", new BigDecimal("3.33")),
            Map.entry("MYR-USD", new BigDecimal("0.22")),
            Map.entry("USD-MYR", new BigDecimal("4.55")),
            Map.entry("MYR-EUR", new BigDecimal("0.20")),
            Map.entry("EUR-MYR", new BigDecimal("5.00")),
            Map.entry("MYR-GBP", new BigDecimal("0.17")),
            Map.entry("GBP-MYR", new BigDecimal("5.88")),
            Map.entry("SGD-USD", new BigDecimal("0.74")),
            Map.entry("USD-SGD", new BigDecimal("1.35")),
            Map.entry("SGD-EUR", new BigDecimal("0.68")),
            Map.entry("EUR-SGD", new BigDecimal("1.47")),
            Map.entry("SGD-GBP", new BigDecimal("0.58")),
            Map.entry("GBP-SGD", new BigDecimal("1.72")),
            Map.entry("USD-EUR", new BigDecimal("0.92")),
            Map.entry("EUR-USD", new BigDecimal("1.09")),
            Map.entry("USD-GBP", new BigDecimal("0.79")),
            Map.entry("GBP-USD", new BigDecimal("1.27")),
            Map.entry("EUR-GBP", new BigDecimal("0.86")),
            Map.entry("GBP-EUR", new BigDecimal("1.16"))
    );

    @Override
    public BigDecimal getRate(Currency from, Currency to) {
        if (from == to) {
            return BigDecimal.ONE;
        }
        BigDecimal rate = RATES.get(from.name() + "-" + to.name());
        if (rate == null) {
            throw new IllegalArgumentException("No rate configured for " + from + " -> " + to);
        }
        return rate;
    }
}
