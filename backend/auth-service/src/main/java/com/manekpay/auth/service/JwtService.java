package com.manekpay.auth.service;

import com.manekpay.auth.entity.KycStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtService {

    public static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;

    public JwtService(RSAPrivateKey privateKey, RSAPublicKey publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    // NOTE: the `kycStatus` claim below is a snapshot at token-issue time only — it does
    // NOT update when a customer's KYC status changes mid-token-lifetime (e.g. auto-approval
    // completing). Never use this claim to gate an authorization decision; always re-check
    // the customer's live kycStatus in the database for anything that matters (e.g. before
    // allowing a financial transaction in a later phase).
    public String issueAccessToken(UUID customerId, String email, KycStatus kycStatus, String homeCurrency) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ACCESS_TOKEN_TTL.toMillis());
        return Jwts.builder()
                .setSubject(customerId.toString())
                .claim("email", email)
                .claim("kycStatus", kycStatus.name())
                .claim("homeCurrency", homeCurrency)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public String issueRefreshTokenId() {
        return UUID.randomUUID().toString();
    }

    public Optional<Claims> validate(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
