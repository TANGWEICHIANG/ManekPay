package com.manekpay.ledger.service;

import com.manekpay.ledger.dto.FxRateResponse;
import com.manekpay.ledger.entity.Currency;
import com.manekpay.ledger.exception.FxServiceUnavailableException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

@Component
public class FxServiceClient implements FxRateProvider {

    private final RestClient restClient;

    public FxServiceClient(@Value("${app.fx-service.base-url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    @Override
    public BigDecimal getRate(Currency from, Currency to, String bearerToken) {
        if (from == to) {
            return BigDecimal.ONE;
        }
        try {
            FxRateResponse response = restClient.get()
                    .uri("/rates/{from}/{to}", from, to)
                    .header("Authorization", "Bearer " + bearerToken)
                    .retrieve()
                    .body(FxRateResponse.class);
            return response != null ? response.rate() : null;
        } catch (RestClientException e) {
            throw new FxServiceUnavailableException(e);
        }
    }
}
