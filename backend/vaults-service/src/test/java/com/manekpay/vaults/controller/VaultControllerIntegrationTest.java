package com.manekpay.vaults.controller;

import com.manekpay.vaults.dto.TransactionCreatedEvent;
import com.manekpay.vaults.entity.Currency;
import com.manekpay.vaults.service.TransactionCreatedListener;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = TransactionCreatedListener.TOPIC)
class VaultControllerIntegrationTest {

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
    private KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate;

    @Test
    void consumingATransactionCreatedEventLazilyCreatesAVaultAndAppliesTheRoundUp() throws Exception {
        String customerSubject = UUID.randomUUID().toString();
        UUID customerId = UUID.fromString(customerSubject);
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                UUID.randomUUID(), customerId, new BigDecimal("12.3000"), Currency.MYR, Currency.MYR, Instant.now());

        kafkaTemplate.send(TransactionCreatedListener.TOPIC, customerId.toString(), event);

        String responseBody = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            var result = mockMvc.perform(get("/me").with(jwt().jwt(j -> j.subject(customerSubject)))).andReturn();
            if (result.getResponse().getStatus() == 200) {
                responseBody = result.getResponse().getContentAsString();
                break;
            }
            Thread.sleep(500);
        }

        assertThat(responseBody).isNotNull();
        mockMvc.perform(get("/me").with(jwt().jwt(j -> j.subject(customerSubject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("MYR"))
                .andExpect(jsonPath("$.balance").value(0.7));
    }
}
