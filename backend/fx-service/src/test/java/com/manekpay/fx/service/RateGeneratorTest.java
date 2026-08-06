package com.manekpay.fx.service;

import com.manekpay.fx.entity.Currency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateGeneratorTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Test
    void refreshRatesWritesAJitteredValueForEveryConfiguredPair() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        RateGenerator generator = new RateGenerator(redisTemplate);
        generator.refreshRates();

        verify(valueOps).set(org.mockito.ArgumentMatchers.eq(RateGenerator.redisKey(Currency.MYR, Currency.SGD)), anyString());
        verify(valueOps).set(org.mockito.ArgumentMatchers.eq(RateGenerator.redisKey(Currency.GBP, Currency.EUR)), anyString());
    }

    @Test
    void jitteredRateStaysWithinTwoPercentOfTheBaseRate() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        RateGenerator generator = new RateGenerator(redisTemplate);
        generator.refreshRates();

        org.mockito.ArgumentCaptor<String> valueCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(org.mockito.ArgumentMatchers.eq(RateGenerator.redisKey(Currency.MYR, Currency.SGD)), valueCaptor.capture());
        BigDecimal written = new BigDecimal(valueCaptor.getValue());

        assertThat(written).isCloseTo(new BigDecimal("0.30"), org.assertj.core.data.Percentage.withPercentage(2.5));
    }
}
