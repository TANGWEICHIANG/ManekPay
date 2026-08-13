package com.manekpay.auth.service;

import com.manekpay.auth.exception.InvalidServiceCredentialsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class ServiceTokenService {

    // Only one service client exists today. If a second one is ever needed, this becomes a map
    // keyed by clientId — not built now (YAGNI) since there's nothing to look up yet.
    private static final String KNOWN_CLIENT_ID = "vaults-service";
    private static final String SCOPE = "vault-sweep";

    private final JwtService jwtService;
    private final String vaultsServiceSecret;

    public ServiceTokenService(JwtService jwtService,
                                @Value("${app.service-credentials.vaults-service-secret}") String vaultsServiceSecret) {
        this.jwtService = jwtService;
        this.vaultsServiceSecret = vaultsServiceSecret;
    }

    public String issueToken(String clientId, String clientSecret) {
        if (!KNOWN_CLIENT_ID.equals(clientId) || !constantTimeEquals(clientSecret, vaultsServiceSecret)) {
            throw new InvalidServiceCredentialsException();
        }
        return jwtService.issueServiceToken(clientId, SCOPE);
    }

    // security.md: use constant-time comparison for secrets and tokens.
    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
