package com.manekpay.vaults.service;

import com.manekpay.vaults.entity.Currency;
import com.manekpay.vaults.exception.InsufficientBalanceException;
import com.manekpay.vaults.exception.LedgerServiceUnavailableException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LedgerServiceClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private LedgerServiceClient clientReturning(int status, String body) throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/wallets/debit", exchange -> {
            byte[] responseBytes = body.getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.close();
        });
        server.start();
        int port = server.getAddress().getPort();

        ServiceTokenProvider tokenProvider = new ServiceTokenProvider("http://localhost:" + port, "unused") {
            @Override
            public synchronized String getToken() {
                return "fake-token";
            }
        };
        return new LedgerServiceClient("http://localhost:" + port, tokenProvider);
    }

    @Test
    void succeedsOn200() throws Exception {
        LedgerServiceClient client = clientReturning(200, "{\"balance\":\"50.0000\"}");
        client.debitWallet(UUID.randomUUID(), Currency.MYR, new BigDecimal("10.00"), "ref-1");
        // No exception thrown = success.
    }

    @Test
    void throwsInsufficientBalanceExceptionOn422() throws Exception {
        LedgerServiceClient client = clientReturning(422, "{\"message\":\"Insufficient balance\"}");
        assertThatThrownBy(() -> client.debitWallet(UUID.randomUUID(), Currency.MYR, new BigDecimal("10.00"), "ref-2"))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    void throwsLedgerServiceUnavailableExceptionOn500() throws Exception {
        LedgerServiceClient client = clientReturning(500, "{\"message\":\"boom\"}");
        assertThatThrownBy(() -> client.debitWallet(UUID.randomUUID(), Currency.MYR, new BigDecimal("10.00"), "ref-3"))
                .isInstanceOf(LedgerServiceUnavailableException.class);
    }
}
