package com.manekpay.ledger.controller;

import com.manekpay.ledger.dto.TransactionCreatedEvent;
import com.manekpay.ledger.entity.Currency;
import com.manekpay.ledger.repository.WalletRepository;
import com.manekpay.ledger.service.AccountService;
import com.manekpay.ledger.service.AuthServiceClient;
import com.manekpay.ledger.service.TransactionEventPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TransferControllerIntegrationTest {

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
    @MockBean
    private AuthServiceClient authServiceClient;
    @MockBean
    private TransactionEventPublisher eventPublisher;

    @Test
    void repeatingTheSameIdempotencyKeyReplaysTheCachedResponseInsteadOfTransferringTwice() throws Exception {
        when(authServiceClient.getLiveKycStatus(any())).thenReturn("APPROVED");
        String senderSubject = UUID.randomUUID().toString();
        UUID sender = UUID.fromString(senderSubject);
        UUID recipient = UUID.randomUUID();
        var senderAccount = accountService.getOrCreateAccount(sender);
        String recipientAccountNumber = accountService.getOrCreateAccount(recipient).getAccountNumber();
        var senderWallet = walletRepository.findByAccountIdAndCurrency(senderAccount.getId(), Currency.MYR).orElseThrow();
        senderWallet.setBalance(new BigDecimal("100.0000"));
        walletRepository.save(senderWallet);

        String requestBody = """
                {"recipient":{"type":"ACCOUNT_NUMBER","value":"%s"},"sourceCurrency":"MYR","destCurrency":"MYR","amount":"30.0000"}
                """.formatted(recipientAccountNumber);

        mockMvc.perform(post("/transfers").with(jwt().jwt(j -> j.subject(senderSubject).claim("homeCurrency", "MYR")))
                        .header("X-Idempotency-Key", "same-key-123")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/transfers").with(jwt().jwt(j -> j.subject(senderSubject).claim("homeCurrency", "MYR")))
                        .header("X-Idempotency-Key", "same-key-123")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated());

        // Only one 30.0000 debit should have actually happened, despite two identical requests.
        var reloaded = walletRepository.findByAccountIdAndCurrency(senderAccount.getId(), Currency.MYR).orElseThrow();
        assertThat(reloaded.getBalance()).isEqualByComparingTo("70.0000");

        // The idempotency filter short-circuits the replay before it reaches the controller, so
        // the transaction.created event must only have been published once, not twice, and with
        // the correct payload for the transfer that actually happened.
        ArgumentCaptor<TransactionCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TransactionCreatedEvent.class);
        verify(eventPublisher, times(1)).publishTransactionCreated(eventCaptor.capture());
        TransactionCreatedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.customerId()).isEqualTo(sender);
        assertThat(publishedEvent.amount()).isEqualByComparingTo("30.0000");
        assertThat(publishedEvent.currency()).isEqualTo(Currency.MYR);
        assertThat(publishedEvent.homeCurrency()).isEqualTo(Currency.MYR);
    }

    @Test
    void listAndGetReturnTheCallersOwnTransfers() throws Exception {
        when(authServiceClient.getLiveKycStatus(any())).thenReturn("APPROVED");
        String senderSubject = UUID.randomUUID().toString();
        UUID sender = UUID.fromString(senderSubject);
        UUID recipient = UUID.randomUUID();
        var senderAccount = accountService.getOrCreateAccount(sender);
        String recipientAccountNumber = accountService.getOrCreateAccount(recipient).getAccountNumber();
        var senderWallet = walletRepository.findByAccountIdAndCurrency(senderAccount.getId(), Currency.MYR).orElseThrow();
        senderWallet.setBalance(new BigDecimal("50.0000"));
        walletRepository.save(senderWallet);

        String requestBody = """
                {"recipient":{"type":"ACCOUNT_NUMBER","value":"%s"},"sourceCurrency":"MYR","destCurrency":"MYR","amount":"10.0000"}
                """.formatted(recipientAccountNumber);

        mockMvc.perform(post("/transfers").with(jwt().jwt(j -> j.subject(senderSubject)))
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/transfers").with(jwt().jwt(j -> j.subject(senderSubject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transfers", org.hamcrest.Matchers.hasSize(1)));

        verify(eventPublisher, times(1)).publishTransactionCreated(any());
    }

    @Test
    void transferWithLocationPublishesEventCarryingTheCoordinates() throws Exception {
        when(authServiceClient.getLiveKycStatus(any())).thenReturn("APPROVED");
        String senderSubject = UUID.randomUUID().toString();
        UUID sender = UUID.fromString(senderSubject);
        UUID recipient = UUID.randomUUID();
        var senderAccount = accountService.getOrCreateAccount(sender);
        String recipientAccountNumber = accountService.getOrCreateAccount(recipient).getAccountNumber();
        var senderWallet = walletRepository.findByAccountIdAndCurrency(senderAccount.getId(), Currency.MYR).orElseThrow();
        senderWallet.setBalance(new BigDecimal("100.0000"));
        walletRepository.save(senderWallet);

        String requestBody = """
                {"recipient":{"type":"ACCOUNT_NUMBER","value":"%s"},"sourceCurrency":"MYR","destCurrency":"MYR","amount":"10.0000","location":{"latitude":3.139,"longitude":101.6869}}
                """.formatted(recipientAccountNumber);

        mockMvc.perform(post("/transfers").with(jwt().jwt(j -> j.subject(senderSubject)))
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated());

        ArgumentCaptor<TransactionCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TransactionCreatedEvent.class);
        verify(eventPublisher).publishTransactionCreated(eventCaptor.capture());
        assertThat(eventCaptor.getValue().latitude()).isEqualTo(3.139);
        assertThat(eventCaptor.getValue().longitude()).isEqualTo(101.6869);
    }

    @Test
    void transferWithNoLocationPublishesEventWithNullCoordinates() throws Exception {
        when(authServiceClient.getLiveKycStatus(any())).thenReturn("APPROVED");
        String senderSubject = UUID.randomUUID().toString();
        UUID sender = UUID.fromString(senderSubject);
        UUID recipient = UUID.randomUUID();
        var senderAccount = accountService.getOrCreateAccount(sender);
        String recipientAccountNumber = accountService.getOrCreateAccount(recipient).getAccountNumber();
        var senderWallet = walletRepository.findByAccountIdAndCurrency(senderAccount.getId(), Currency.MYR).orElseThrow();
        senderWallet.setBalance(new BigDecimal("100.0000"));
        walletRepository.save(senderWallet);

        String requestBody = """
                {"recipient":{"type":"ACCOUNT_NUMBER","value":"%s"},"sourceCurrency":"MYR","destCurrency":"MYR","amount":"10.0000"}
                """.formatted(recipientAccountNumber);

        mockMvc.perform(post("/transfers").with(jwt().jwt(j -> j.subject(senderSubject)))
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated());

        ArgumentCaptor<TransactionCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TransactionCreatedEvent.class);
        verify(eventPublisher).publishTransactionCreated(eventCaptor.capture());
        assertThat(eventCaptor.getValue().latitude()).isNull();
        assertThat(eventCaptor.getValue().longitude()).isNull();
    }
}
