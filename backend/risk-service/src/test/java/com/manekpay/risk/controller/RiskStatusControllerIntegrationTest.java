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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = TransactionCreatedListener.TOPIC)
class RiskStatusControllerIntegrationTest {

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
    void reportsNotRestrictedWhenTheCustomerHasNoFlags() throws Exception {
        UUID customerId = UUID.randomUUID();

        mockMvc.perform(get("/internal/risk-status/{customerId}", customerId)
                        .with(jwt().jwt(j -> j.subject("ledger-service").claim("scope", "risk-check"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restricted").value(false))
                .andExpect(jsonPath("$.restrictedUntil").doesNotExist());
    }

    @Test
    void reportsRestrictedWhenAFlagIsWithinTheLast24Hours() throws Exception {
        UUID customerId = UUID.randomUUID();
        // Truncated to microseconds: Postgres TIMESTAMPTZ only stores microsecond precision, so a
        // sub-microsecond Instant.now() would round-trip through the DB with a different string
        // representation than the in-memory value asserted below.
        Instant flaggedAt = Instant.now().truncatedTo(ChronoUnit.MICROS).minus(1, ChronoUnit.HOURS);
        riskFlagRepository.save(flagAt(customerId, flaggedAt));

        mockMvc.perform(get("/internal/risk-status/{customerId}", customerId)
                        .with(jwt().jwt(j -> j.subject("ledger-service").claim("scope", "risk-check"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restricted").value(true))
                .andExpect(jsonPath("$.restrictedUntil").value(flaggedAt.plus(24, ChronoUnit.HOURS).toString()));
    }

    @Test
    void reportsRestrictedWhenAFlagIsExactlyAtThe24HourBoundary() throws Exception {
        UUID customerId = UUID.randomUUID();
        Instant flaggedAt = Instant.now().truncatedTo(ChronoUnit.MICROS).minus(24, ChronoUnit.HOURS).plus(1, ChronoUnit.SECONDS);
        riskFlagRepository.save(flagAt(customerId, flaggedAt));

        mockMvc.perform(get("/internal/risk-status/{customerId}", customerId)
                        .with(jwt().jwt(j -> j.subject("ledger-service").claim("scope", "risk-check"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restricted").value(true));
    }

    @Test
    void reportsNotRestrictedWhenTheOnlyFlagIsMoreThan24HoursOld() throws Exception {
        UUID customerId = UUID.randomUUID();
        Instant flaggedAt = Instant.now().minus(24, ChronoUnit.HOURS).minus(1, ChronoUnit.MINUTES);
        riskFlagRepository.save(flagAt(customerId, flaggedAt));

        mockMvc.perform(get("/internal/risk-status/{customerId}", customerId)
                        .with(jwt().jwt(j -> j.subject("ledger-service").claim("scope", "risk-check"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restricted").value(false));
    }

    @Test
    void rejectsATokenWithoutTheRiskCheckScopeWith403() throws Exception {
        mockMvc.perform(get("/internal/risk-status/{customerId}", UUID.randomUUID())
                        .with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isForbidden());
    }

    private RiskFlag flagAt(UUID customerId, Instant createdAt) {
        RiskFlag flag = new RiskFlag(customerId, UUID.randomUUID(), "VELOCITY", "test flag");
        org.springframework.test.util.ReflectionTestUtils.setField(flag, "createdAt", createdAt);
        return flag;
    }
}
