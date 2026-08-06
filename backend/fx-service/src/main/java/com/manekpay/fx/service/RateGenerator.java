package com.manekpay.fx.service;

import com.manekpay.fx.entity.Currency;

import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.Map;

@Component
public class RateGenerator {

    // Fixed illustrative base rates (matches ledger-service's prior StubFxRateProvider), jittered
    // +/-2% on every refresh to look realistically "live" without calling a real market data API.
    private static final Map<String, BigDecimal> BASE_RATES = Map.ofEntries(
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

    static final String REDIS_KEY_PREFIX = "fx:rate:";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;

    public RateGenerator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    @Scheduled(fixedRate = 10000)
    public void refreshRates() {
        BASE_RATES.forEach((pair, base) -> {
            double jitterFactor = 1 + (RANDOM.nextDouble() * 0.04 - 0.02);
            BigDecimal jittered = base.multiply(BigDecimal.valueOf(jitterFactor)).setScale(4, RoundingMode.HALF_EVEN);
            redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + pair, jittered.toPlainString());
        });
    }

    public static String redisKey(Currency from, Currency to) {
        return REDIS_KEY_PREFIX + from.name() + "-" + to.name();
    }
}
