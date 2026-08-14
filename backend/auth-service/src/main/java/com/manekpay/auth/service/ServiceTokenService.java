package com.manekpay.auth.service;

import com.manekpay.auth.exception.InvalidServiceCredentialsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@Service
public class ServiceTokenService {

    private final JwtService jwtService;
    private final Map<String, ServiceClient> knownClients;

    public ServiceTokenService(JwtService jwtService,
                                @Value("${app.service-credentials.vaults-service-secret}") String vaultsServiceSecret,
                                @Value("${app.service-credentials.ledger-service-secret}") String ledgerServiceSecret) {
        this.jwtService = jwtService;
        this.knownClients = Map.of(
                "vaults-service", new ServiceClient(vaultsServiceSecret, "vault-sweep"),
                "ledger-service", new ServiceClient(ledgerServiceSecret, "risk-check"));
    }

    public String issueToken(String clientId, String clientSecret) {
        ServiceClient client = knownClients.get(clientId);
        if (client == null || !constantTimeEquals(clientSecret, client.secret())) {
            throw new InvalidServiceCredentialsException();
        }
        return jwtService.issueServiceToken(clientId, client.scope());
    }

    // security.md: use constant-time comparison for secrets and tokens.
    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private record ServiceClient(String secret, String scope) {
    }
}
