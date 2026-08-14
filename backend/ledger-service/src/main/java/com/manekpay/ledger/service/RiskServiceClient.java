package com.manekpay.ledger.service;

import com.manekpay.ledger.dto.RiskStatusResponse;
import com.manekpay.ledger.exception.RiskServiceUnavailableException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class RiskServiceClient {

    private final RestClient restClient;
    private final ServiceTokenProvider serviceTokenProvider;

    public RiskServiceClient(@Value("${app.risk-service.base-url}") String riskServiceBaseUrl,
                              ServiceTokenProvider serviceTokenProvider) {
        this.restClient = RestClient.create(riskServiceBaseUrl);
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
