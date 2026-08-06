package com.manekpay.ledger.service;

import com.manekpay.ledger.entity.Currency;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FxServiceClientTest {

    @Test
    void returnsOneWhenFromAndToAreTheSameCurrencyWithoutCallingFxService() {
        FxServiceClient client = new FxServiceClient("http://localhost:1");

        assertThat(client.getRate(Currency.MYR, Currency.MYR, "any-token")).isEqualByComparingTo(BigDecimal.ONE);
    }
}
