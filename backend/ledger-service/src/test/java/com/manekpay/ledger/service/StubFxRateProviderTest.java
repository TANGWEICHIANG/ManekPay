package com.manekpay.ledger.service;

import com.manekpay.ledger.entity.Currency;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class StubFxRateProviderTest {

    private final StubFxRateProvider provider = new StubFxRateProvider();

    @Test
    void sameCurrencyRateIsAlwaysOne() {
        assertThat(provider.getRate(Currency.MYR, Currency.MYR)).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(provider.getRate(Currency.USD, Currency.USD)).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void everyCurrencyPairHasARate() {
        for (Currency from : Currency.values()) {
            for (Currency to : Currency.values()) {
                assertThat(provider.getRate(from, to)).isNotNull();
            }
        }
    }

    @Test
    void rateIsPositive() {
        assertThat(provider.getRate(Currency.MYR, Currency.USD)).isGreaterThan(BigDecimal.ZERO);
        assertThat(provider.getRate(Currency.USD, Currency.MYR)).isGreaterThan(BigDecimal.ZERO);
    }
}
