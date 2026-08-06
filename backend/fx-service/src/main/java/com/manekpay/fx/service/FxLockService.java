package com.manekpay.fx.service;

import com.manekpay.fx.entity.Currency;
import com.manekpay.fx.exception.LockNotFoundException;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class FxLockService {

    private static final String REDIS_KEY_PREFIX = "fx:lock:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(15);

    private final StringRedisTemplate redisTemplate;
    private final FxRateService fxRateService;

    public FxLockService(StringRedisTemplate redisTemplate, FxRateService fxRateService) {
        this.redisTemplate = redisTemplate;
        this.fxRateService = fxRateService;
    }

    public FxLock createLock(Currency from, Currency to) {
        BigDecimal rate = fxRateService.getRate(from, to);
        String lockId = UUID.randomUUID().toString();
        String value = from.name() + "|" + to.name() + "|" + rate.toPlainString();
        redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + lockId, value, LOCK_TTL);
        return new FxLock(lockId, from, to, rate, Instant.now().plus(LOCK_TTL));
    }

    // expiresAt is reconstructed from the Redis TTL remaining on this key, so a stored lock
    // never reports a later expiry than Redis will actually honor.
    public FxLock getLock(String lockId) {
        String value = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + lockId);
        if (value == null) {
            throw new LockNotFoundException();
        }
        Long ttlSeconds = redisTemplate.getExpire(REDIS_KEY_PREFIX + lockId);
        String[] parts = value.split("\\|");
        Currency from = Currency.valueOf(parts[0]);
        Currency to = Currency.valueOf(parts[1]);
        BigDecimal rate = new BigDecimal(parts[2]);
        Instant expiresAt = Instant.now().plusSeconds(ttlSeconds != null ? ttlSeconds : 0);
        return new FxLock(lockId, from, to, rate, expiresAt);
    }

    public record FxLock(String lockId, Currency from, Currency to, BigDecimal rate, Instant expiresAt) {
    }
}
