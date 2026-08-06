package com.manekpay.auth.service;

import com.manekpay.auth.config.JwtKeyConfig;
import com.manekpay.auth.entity.KycStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService newServiceWithFreshKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        return new JwtService((RSAPrivateKey) pair.getPrivate(), (RSAPublicKey) pair.getPublic());
    }

    @Test
    void issuedAccessTokenValidatesWithMatchingPublicKey() throws Exception {
        JwtService service = newServiceWithFreshKeyPair();
        UUID customerId = UUID.randomUUID();

        String token = service.issueAccessToken(customerId, "carol@example.com", KycStatus.APPROVED, "MYR");
        Optional<Claims> claims = service.validate(token);

        assertThat(claims).isPresent();
        assertThat(claims.get().getSubject()).isEqualTo(customerId.toString());
        assertThat(claims.get().get("email")).isEqualTo("carol@example.com");
        assertThat(claims.get().get("kycStatus")).isEqualTo("APPROVED");
        assertThat(claims.get().get("homeCurrency")).isEqualTo("MYR");
    }

    @Test
    void tokenSignedByDifferentKeyFailsValidation() throws Exception {
        JwtService signer = newServiceWithFreshKeyPair();
        JwtService verifier = newServiceWithFreshKeyPair();

        String token = signer.issueAccessToken(UUID.randomUUID(), "dave@example.com", KycStatus.PENDING, "MYR");
        Optional<Claims> claims = verifier.validate(token);

        assertThat(claims).isEmpty();
    }

    @Test
    void malformedTokenFailsValidation() throws Exception {
        JwtService service = newServiceWithFreshKeyPair();
        assertThat(service.validate("not-a-real-token")).isEmpty();
    }

    @Test
    void refreshTokenIdIsARandomUuidString() throws Exception {
        JwtService service = newServiceWithFreshKeyPair();
        String id1 = service.issueRefreshTokenId();
        String id2 = service.issueRefreshTokenId();
        assertThat(id1).isNotEqualTo(id2);
        assertThat(UUID.fromString(id1)).isNotNull();
    }
}
