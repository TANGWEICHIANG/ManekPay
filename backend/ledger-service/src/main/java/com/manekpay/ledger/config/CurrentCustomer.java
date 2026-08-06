package com.manekpay.ledger.config;

import com.manekpay.ledger.entity.Currency;
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

    // Defaults to MYR both when the claim is absent (covers the brief transition window where a
    // token issued just before this change is still valid - access tokens expire in 15 minutes,
    // so this self-resolves quickly) and when the claim is present but fails to parse (defensive -
    // this is a purely informational field and must never crash a transfer request over it).
    public static Currency homeCurrency() {
        Object claim = jwt().getClaim("homeCurrency");
        if (claim == null) {
            return Currency.MYR;
        }
        try {
            return Currency.valueOf(claim.toString());
        } catch (IllegalArgumentException e) {
            return Currency.MYR;
        }
    }

    private static Jwt jwt() {
        return (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
