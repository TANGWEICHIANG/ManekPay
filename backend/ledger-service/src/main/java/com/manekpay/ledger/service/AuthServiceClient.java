package com.manekpay.ledger.service;

import com.manekpay.ledger.dto.AuthMeResponse;
import com.manekpay.ledger.exception.AuthServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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
        try {
            AuthMeResponse response = restClient.get()
                    .uri("/me")
                    .header("Authorization", "Bearer " + bearerToken)
                    .retrieve()
                    .body(AuthMeResponse.class);
            return response != null ? response.kycStatus() : null;
        } catch (RestClientException e) {
            // Covers both connectivity failures (auth-service down/unreachable) and non-2xx
            // responses (e.g. the token was valid for ledger-service's own JWKS check but
            // auth-service itself now rejects it, such as after a logout) - either way this
            // is not a "customer isn't KYC-approved" answer, so it must not be swallowed into
            // one and must not surface as an opaque 500 (see ApiExceptionHandler).
            throw new AuthServiceUnavailableException(e);
        }
    }
}
