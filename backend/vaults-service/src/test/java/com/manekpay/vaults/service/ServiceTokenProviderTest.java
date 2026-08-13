package com.manekpay.vaults.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceTokenProviderTest {

    private HttpServer server;
    private final AtomicInteger requestCount = new AtomicInteger();

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private ServiceTokenProvider providerReturning(String token, long expiresInSeconds) throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/service-token", exchange -> {
            requestCount.incrementAndGet();
            byte[] responseBytes = ("{\"accessToken\":\"" + token + "\",\"expiresIn\":" + expiresInSeconds + "}").getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.close();
        });
        server.start();
        int port = server.getAddress().getPort();
        return new ServiceTokenProvider("http://localhost:" + port, "unused");
    }

    @Test
    void fetchesTheTokenFromTheAuthServiceOnFirstCall() throws Exception {
        ServiceTokenProvider provider = providerReturning("token-1", 300);

        assertThat(provider.getToken()).isEqualTo("token-1");
    }

    @Test
    void reusesTheCachedTokenWithoutRefetchingWithinTheCacheWindow() throws Exception {
        ServiceTokenProvider provider = providerReturning("token-1", 300);

        provider.getToken();
        provider.getToken();

        assertThat(requestCount.get()).isEqualTo(1);
    }
}
