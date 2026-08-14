package com.manekpay.ledger.service;

import com.manekpay.ledger.dto.AuthMeResponse;
import com.manekpay.ledger.exception.AuthServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AuthServiceClient {

    // Same-network, service-to-service call - 5s is generous headroom without leaving a request
    // thread parked indefinitely if auth-service hangs instead of refusing the connection.
    private static final int TIMEOUT_MILLIS = 5000;

    private final RestClient restClient;

    public AuthServiceClient(@Value("${app.auth-service.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT_MILLIS);
        requestFactory.setReadTimeout(TIMEOUT_MILLIS);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
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
