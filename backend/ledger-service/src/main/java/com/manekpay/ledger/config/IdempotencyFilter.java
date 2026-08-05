package com.manekpay.ledger.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public IdempotencyFilter(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && "/transfers".equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String idempotencyKey = request.getHeader("X-Idempotency-Key");
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                : null;
        if (idempotencyKey == null || idempotencyKey.isBlank() || !(principal instanceof Jwt jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        String redisKey = "idempotency:" + jwt.getSubject() + ":" + idempotencyKey;
        String cached = redisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            CachedResponse cachedResponse = objectMapper.readValue(cached, CachedResponse.class);
            response.setStatus(cachedResponse.status());
            response.setContentType("application/json");
            response.getWriter().write(cachedResponse.body());
            return;
        }

        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, wrappedResponse);

        if (wrappedResponse.getStatus() < 400) {
            String body = new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);
            CachedResponse toCache = new CachedResponse(wrappedResponse.getStatus(), body);
            redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(toCache), TTL);
        }
        wrappedResponse.copyBodyToResponse();
    }

    private record CachedResponse(int status, String body) {
    }
}
