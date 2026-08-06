package com.manekpay.fx.service;

import com.manekpay.fx.entity.Currency;
import com.manekpay.fx.exception.RateNotFoundException;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class FxRateService {

    private final StringRedisTemplate redisTemplate;

    public FxRateService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public BigDecimal getRate(Currency from, Currency to) {
        if (from == to) {
            return BigDecimal.ONE;
        }
        String value = redisTemplate.opsForValue().get(RateGenerator.redisKey(from, to));
        if (value == null) {
            throw new RateNotFoundException(from + "-" + to);
        }
        return new BigDecimal(value);
    }
}
