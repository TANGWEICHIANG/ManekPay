package com.manekpay.wealth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class WealthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("manekpay")
            .withUsername("manekpay")
            .withPassword("manekpay");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsSeededAssetsFilteredByShariahCompliance() throws Exception {
        mockMvc.perform(get("/assets").param("shariahCompliant", "true").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assets", org.hamcrest.Matchers.hasSize(4)));
    }

    @Test
    void buyingSharesCreatesATradeAndUpdatesHoldings() throws Exception {
        String customerSubject = UUID.randomUUID().toString();
        String requestBody = """
                {"assetSymbol":"AAPL","amount":"190.0000"}
                """;

        mockMvc.perform(post("/trades").with(jwt().jwt(j -> j.subject(customerSubject)))
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shares").value(1.0));

        mockMvc.perform(get("/holdings/me").with(jwt().jwt(j -> j.subject(customerSubject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holdings", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.holdings[0].assetSymbol").value("AAPL"));
    }
}
