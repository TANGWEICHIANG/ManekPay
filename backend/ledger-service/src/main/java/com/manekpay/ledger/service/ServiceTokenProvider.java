package com.manekpay.ledger.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;

@Component
public class ServiceTokenProvider {

    // Same-network, service-to-service call - 5s is generous headroom without leaving a request
    // thread parked indefinitely if auth-service hangs instead of refusing the connection.
    private static final int TIMEOUT_MILLIS = 5000;

    private final RestClient restClient;
    private final String clientSecret;

    private volatile String cachedToken;
    private volatile Instant cachedTokenExpiry = Instant.EPOCH;

    public ServiceTokenProvider(@Value("${app.auth-service.base-url}") String authServiceBaseUrl,
                                 @Value("${app.service-credentials.client-secret}") String clientSecret) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT_MILLIS);
        requestFactory.setReadTimeout(TIMEOUT_MILLIS);
        this.restClient = RestClient.builder().baseUrl(authServiceBaseUrl).requestFactory(requestFactory).build();
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
