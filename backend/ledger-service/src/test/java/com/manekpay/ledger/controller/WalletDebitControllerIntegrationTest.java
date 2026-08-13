package com.manekpay.ledger.controller;

import com.manekpay.ledger.entity.Currency;
import com.manekpay.ledger.repository.WalletRepository;
import com.manekpay.ledger.service.AccountService;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class WalletDebitControllerIntegrationTest {

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
    @Autowired
    private AccountService accountService;
    @Autowired
    private WalletRepository walletRepository;

    @Test
    void debitsTheCustomersWalletWhenCalledWithAValidVaultSweepScope() throws Exception {
        UUID customerId = UUID.randomUUID();
        var account = accountService.getOrCreateAccount(customerId);
        var wallet = walletRepository.findByAccountIdAndCurrency(account.getId(), Currency.MYR).orElseThrow();
        wallet.setBalance(new BigDecimal("100.0000"));
        walletRepository.save(wallet);

        String requestBody = """
                {"customerId":"%s","currency":"MYR","amount":"25.0000"}
                """.formatted(customerId);

        mockMvc.perform(post("/internal/wallets/debit")
                        .with(jwt().jwt(j -> j.subject("vaults-service").claim("scope", "vault-sweep")))
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(75.0));

        var reloaded = walletRepository.findByAccountIdAndCurrency(account.getId(), Currency.MYR).orElseThrow();
        assertThat(reloaded.getBalance()).isEqualByComparingTo("75.0000");
    }

    @Test
    void rejectsATokenWithoutTheVaultSweepScopeWith403() throws Exception {
        UUID customerId = UUID.randomUUID();
        accountService.getOrCreateAccount(customerId);
        String requestBody = """
                {"customerId":"%s","currency":"MYR","amount":"10.0000"}
                """.formatted(customerId);

        mockMvc.perform(post("/internal/wallets/debit")
                        .with(jwt().jwt(j -> j.subject(customerId.toString())))
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void returns422WhenBalanceIsInsufficient() throws Exception {
        UUID customerId = UUID.randomUUID();
        accountService.getOrCreateAccount(customerId);
        String requestBody = """
                {"customerId":"%s","currency":"MYR","amount":"999999.0000"}
                """.formatted(customerId);

        mockMvc.perform(post("/internal/wallets/debit")
                        .with(jwt().jwt(j -> j.subject("vaults-service").claim("scope", "vault-sweep")))
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isUnprocessableEntity());
    }
}
