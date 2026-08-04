package com.manekpay.auth.auth;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    public static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);
    private static final String KEY_PREFIX = "refresh_token:";

    private final StringRedisTemplate redisTemplate;
    private final JwtService jwtService;

    public RefreshTokenService(StringRedisTemplate redisTemplate, JwtService jwtService) {
        this.redisTemplate = redisTemplate;
        this.jwtService = jwtService;
    }

    public String issue(UUID customerId) {
        String tokenId = jwtService.issueRefreshTokenId();
        redisTemplate.opsForValue().set(KEY_PREFIX + tokenId, customerId.toString(), REFRESH_TOKEN_TTL);
        return tokenId;
    }

    public Optional<UUID> consume(String tokenId) {
        String key = KEY_PREFIX + tokenId;
        String customerId = redisTemplate.opsForValue().get(key);
        if (customerId == null) {
            return Optional.empty();
        }
        redisTemplate.delete(key);
        return Optional.of(UUID.fromString(customerId));
    }
}
