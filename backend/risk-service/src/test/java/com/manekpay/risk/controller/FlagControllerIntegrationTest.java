package com.manekpay.risk.controller;

import com.manekpay.risk.entity.RiskFlag;
import com.manekpay.risk.repository.RiskFlagRepository;
import com.manekpay.risk.service.TransactionCreatedListener;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = TransactionCreatedListener.TOPIC)
class FlagControllerIntegrationTest {

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
        registry.add("spring.kafka.bootstrap-servers", () -> System.getProperty("spring.embedded.kafka.brokers"));
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RiskFlagRepository riskFlagRepository;

    @Test
    void returnsTheCallersOwnFlagsMostRecentFirst() throws Exception {
        String customerSubject = UUID.randomUUID().toString();
        UUID customerId = UUID.fromString(customerSubject);
        riskFlagRepository.save(new RiskFlag(customerId, UUID.randomUUID(), "VELOCITY", "6 high-value transfers within the last 60 seconds"));

        mockMvc.perform(get("/flags/me").with(jwt().jwt(j -> j.subject(customerSubject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flags", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.flags[0].rule").value("VELOCITY"));
    }
}
