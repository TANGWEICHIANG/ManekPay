package com.manekpay.auth.service;

import com.manekpay.auth.exception.InvalidServiceCredentialsException;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceTokenServiceTest {

    private JwtService newJwtService() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        return new JwtService((RSAPrivateKey) pair.getPrivate(), (RSAPublicKey) pair.getPublic());
    }

    @Test
    void issuesAScopedTokenForValidVaultsServiceCredentials() throws Exception {
        JwtService jwtService = newJwtService();
        ServiceTokenService service = new ServiceTokenService(jwtService, "correct-secret", "irrelevant-ledger-secret");

        String token = service.issueToken("vaults-service", "correct-secret");

        Optional<io.jsonwebtoken.Claims> claims = jwtService.validate(token);
        assertThat(claims).isPresent();
        assertThat(claims.get().getSubject()).isEqualTo("vaults-service");
        assertThat(claims.get().get("scope")).isEqualTo("vault-sweep");
    }

    @Test
    void rejectsAnIncorrectSecret() throws Exception {
        JwtService jwtService = newJwtService();
        ServiceTokenService service = new ServiceTokenService(jwtService, "correct-secret", "irrelevant-ledger-secret");

        assertThatThrownBy(() -> service.issueToken("vaults-service", "wrong-secret"))
                .isInstanceOf(InvalidServiceCredentialsException.class);
    }

    @Test
    void rejectsAnUnrecognizedClientId() throws Exception {
        JwtService jwtService = newJwtService();
        ServiceTokenService service = new ServiceTokenService(jwtService, "correct-secret", "irrelevant-ledger-secret");

        assertThatThrownBy(() -> service.issueToken("some-other-service", "correct-secret"))
                .isInstanceOf(InvalidServiceCredentialsException.class);
    }
}
