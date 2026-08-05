package com.manekpay.ledger.service;

import com.manekpay.ledger.dto.AuthMeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AuthServiceClient {

    private final RestClient restClient;

    public AuthServiceClient(@Value("${app.auth-service.base-url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    // NOTE: this is the only synchronous cross-service call in ledger-service. It exists
    // specifically because auth-service's own JwtService.java documents that the JWT's
    // kycStatus claim is a stale, token-issue-time snapshot that must never be trusted for
    // an authorization decision like this one — see the spec's JWT validation section.
    public String getLiveKycStatus(String bearerToken) {
        AuthMeResponse response = restClient.get()
                .uri("/me")
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve()
                .body(AuthMeResponse.class);
        return response != null ? response.kycStatus() : null;
    }
}
