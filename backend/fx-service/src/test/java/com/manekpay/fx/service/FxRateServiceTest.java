package com.manekpay.fx.service;

import com.manekpay.fx.entity.Currency;
import com.manekpay.fx.exception.RateNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FxRateServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Test
    void returnsOneWhenFromAndToAreTheSameCurrency() {
        FxRateService service = new FxRateService(redisTemplate);
        assertThat(service.getRate(Currency.MYR, Currency.MYR)).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void returnsTheCachedRateWhenPresent() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(RateGenerator.redisKey(Currency.MYR, Currency.SGD))).thenReturn("0.3050");

        FxRateService service = new FxRateService(redisTemplate);

        assertThat(service.getRate(Currency.MYR, Currency.SGD)).isEqualByComparingTo("0.3050");
    }

    @Test
    void throwsWhenNoRateIsCached() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(RateGenerator.redisKey(Currency.MYR, Currency.SGD))).thenReturn(null);

        FxRateService service = new FxRateService(redisTemplate);

        assertThatThrownBy(() -> service.getRate(Currency.MYR, Currency.SGD))
                .isInstanceOf(RateNotFoundException.class);
    }
}
