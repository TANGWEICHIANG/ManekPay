package com.manekpay.ledger.service;

import com.manekpay.ledger.dto.RiskStatusResponse;
import com.manekpay.ledger.exception.RiskServiceUnavailableException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class RiskServiceClient {

    // Same-network, service-to-service call - 5s is generous headroom without leaving a request
    // thread parked indefinitely if risk-service hangs instead of refusing the connection.
    private static final int TIMEOUT_MILLIS = 5000;

    private final RestClient restClient;
    private final ServiceTokenProvider serviceTokenProvider;

    public RiskServiceClient(@Value("${app.risk-service.base-url}") String riskServiceBaseUrl,
                              ServiceTokenProvider serviceTokenProvider) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT_MILLIS);
        requestFactory.setReadTimeout(TIMEOUT_MILLIS);
        this.restClient = RestClient.builder().baseUrl(riskServiceBaseUrl).requestFactory(requestFactory).build();
        this.serviceTokenProvider = serviceTokenProvider;
    }

    public RiskStatusResponse getRiskStatus(UUID customerId) {
        try {
            RiskStatusResponse response = restClient.get()
                    .uri("/internal/risk-status/{customerId}", customerId)
                    .header("Authorization", "Bearer " + serviceTokenProvider.getToken())
                    .retrieve()
                    .body(RiskStatusResponse.class);
            if (response == null) {
                // An empty/malformed body must fail closed like any other unavailability, not
                // silently fall through as an unrestricted RiskStatusResponse.
                throw new RiskServiceUnavailableException(new IllegalStateException("risk-service returned an empty response body"));
            }
            return response;
        } catch (RestClientException e) {
            throw new RiskServiceUnavailableException(e);
        }
    }
}
