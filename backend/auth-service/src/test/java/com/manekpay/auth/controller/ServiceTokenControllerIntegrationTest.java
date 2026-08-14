package com.manekpay.auth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ServiceTokenControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("manekpay")
            .withUsername("manekpay")
            .withPassword("manekpay");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.service-credentials.vaults-service-secret", () -> "test-secret");
        registry.add("app.service-credentials.ledger-service-secret", () -> "test-ledger-secret");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void issuesATokenForValidCredentials() throws Exception {
        mockMvc.perform(post("/service-token")
                        .contentType("application/json")
                        .content("{\"clientId\":\"vaults-service\",\"clientSecret\":\"test-secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(300));
    }

    @Test
    void rejectsAnIncorrectSecretWith401() throws Exception {
        mockMvc.perform(post("/service-token")
                        .contentType("application/json")
                        .content("{\"clientId\":\"vaults-service\",\"clientSecret\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void issuesATokenForLedgerServiceWithValidCredentials() throws Exception {
        mockMvc.perform(post("/service-token")
                        .contentType("application/json")
                        .content("{\"clientId\":\"ledger-service\",\"clientSecret\":\"test-ledger-secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(300));
    }

    @Test
    void rejectsAnUnknownClientIdWith401() throws Exception {
        mockMvc.perform(post("/service-token")
                        .contentType("application/json")
                        .content("{\"clientId\":\"not-a-real-service\",\"clientSecret\":\"anything\"}"))
                .andExpect(status().isUnauthorized());
    }
}
