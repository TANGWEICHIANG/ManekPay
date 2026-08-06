package com.manekpay.fx.service;

import com.manekpay.fx.entity.Currency;
import com.manekpay.fx.exception.LockNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FxLockServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private FxRateService fxRateService;

    @Test
    void createLockStoresTheCurrentRateWithA15SecondTtl() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(fxRateService.getRate(Currency.MYR, Currency.SGD)).thenReturn(new BigDecimal("0.3050"));

        FxLockService service = new FxLockService(redisTemplate, fxRateService);
        FxLockService.FxLock lock = service.createLock(Currency.MYR, Currency.SGD);

        assertThat(lock.from()).isEqualTo(Currency.MYR);
        assertThat(lock.to()).isEqualTo(Currency.SGD);
        assertThat(lock.rate()).isEqualByComparingTo("0.3050");
        verify(valueOps).set(anyString(), anyString(), org.mockito.ArgumentMatchers.eq(Duration.ofSeconds(15)));
    }

    @Test
    void getLockReturnsTheStoredLockWhenPresent() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("fx:lock:abc-123")).thenReturn("MYR|SGD|0.3050");
        when(redisTemplate.getExpire("fx:lock:abc-123")).thenReturn(10L);

        FxLockService service = new FxLockService(redisTemplate, fxRateService);
        FxLockService.FxLock lock = service.getLock("abc-123");

        assertThat(lock.from()).isEqualTo(Currency.MYR);
        assertThat(lock.to()).isEqualTo(Currency.SGD);
        assertThat(lock.rate()).isEqualByComparingTo("0.3050");
    }

    @Test
    void getLockThrowsWhenMissingOrExpired() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("fx:lock:gone")).thenReturn(null);

        FxLockService service = new FxLockService(redisTemplate, fxRateService);

        assertThatThrownBy(() -> service.getLock("gone")).isInstanceOf(LockNotFoundException.class);
    }

    @Test
    void getLockThrowsWhenKeyExpiredBetweenGetAndGetExpire() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("fx:lock:racy")).thenReturn("MYR|SGD|0.3050");
        when(redisTemplate.getExpire("fx:lock:racy")).thenReturn(-2L);

        FxLockService service = new FxLockService(redisTemplate, fxRateService);

        assertThatThrownBy(() -> service.getLock("racy")).isInstanceOf(LockNotFoundException.class);
    }
}
