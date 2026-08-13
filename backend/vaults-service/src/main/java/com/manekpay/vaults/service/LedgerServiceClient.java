package com.manekpay.vaults.service;

import com.manekpay.vaults.entity.Currency;
import com.manekpay.vaults.exception.InsufficientBalanceException;
import com.manekpay.vaults.exception.LedgerServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class LedgerServiceClient {

    private final RestClient restClient;
    private final ServiceTokenProvider serviceTokenProvider;

    public LedgerServiceClient(@Value("${app.ledger-service.base-url}") String ledgerServiceBaseUrl,
                                ServiceTokenProvider serviceTokenProvider) {
        this.restClient = RestClient.create(ledgerServiceBaseUrl);
        this.serviceTokenProvider = serviceTokenProvider;
    }

    public void debitWallet(UUID customerId, Currency currency, BigDecimal amount, String idempotencyKey) {
        try {
            restClient.post()
                    .uri("/internal/wallets/debit")
                    .header("Authorization", "Bearer " + serviceTokenProvider.getToken())
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new DebitRequest(customerId, currency, amount))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.UnprocessableEntity e) {
            throw new InsufficientBalanceException();
        } catch (RestClientException e) {
            throw new LedgerServiceUnavailableException(e);
        }
    }

    private record DebitRequest(UUID customerId, Currency currency, BigDecimal amount) {
    }
}
