package com.manekpay.ledger.config;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public final class CurrentCustomer {

    private CurrentCustomer() {
    }

    public static UUID id() {
        return UUID.fromString(jwt().getSubject());
    }

    public static String bearerToken() {
        return jwt().getTokenValue();
    }

    private static Jwt jwt() {
        return (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
