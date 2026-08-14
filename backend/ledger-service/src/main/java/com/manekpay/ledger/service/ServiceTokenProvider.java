package com.manekpay.ledger.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;

@Component
public class ServiceTokenProvider {

    private final RestClient restClient;
    private final String clientSecret;

    private volatile String cachedToken;
    private volatile Instant cachedTokenExpiry = Instant.EPOCH;

    public ServiceTokenProvider(@Value("${app.auth-service.base-url}") String authServiceBaseUrl,
                                 @Value("${app.service-credentials.client-secret}") String clientSecret) {
        this.restClient = RestClient.create(authServiceBaseUrl);
        this.clientSecret = clientSecret;
    }

    // Cached until 30s before expiry, same rationale as vaults-service's identical component:
    // a burst of transfers reuses one token instead of fetching a fresh one per transfer.
    public synchronized String getToken() {
        if (cachedToken == null || Instant.now().isAfter(cachedTokenExpiry.minusSeconds(30))) {
            ServiceTokenResponse response = restClient.post()
                    .uri("/service-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ServiceTokenRequest("ledger-service", clientSecret))
                    .retrieve()
                    .body(ServiceTokenResponse.class);
            cachedToken = response.accessToken();
            cachedTokenExpiry = Instant.now().plusSeconds(response.expiresIn());
        }
        return cachedToken;
    }

    private record ServiceTokenRequest(String clientId, String clientSecret) {
    }

    private record ServiceTokenResponse(String accessToken, long expiresIn) {
    }
}
