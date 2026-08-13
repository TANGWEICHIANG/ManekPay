package com.manekpay.ledger.service;

import com.manekpay.ledger.dto.RecipientDto;
import com.manekpay.ledger.dto.RecipientType;
import com.manekpay.ledger.dto.TransferRequest;
import com.manekpay.ledger.dto.TransferResponse;
import com.manekpay.ledger.entity.Currency;
import com.manekpay.ledger.exception.InsufficientBalanceException;
import com.manekpay.ledger.exception.KycNotApprovedException;
import com.manekpay.ledger.exception.SelfTransferException;
import com.manekpay.ledger.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
class TransferServiceIntegrationTest {

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
    private TransferService transferService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private WalletRepository walletRepository;
    @MockBean
    private AuthServiceClient authServiceClient;

    @Test
    void sameCurrencyTransferMovesBalanceWithTwoLedgerEntries() {
        when(authServiceClient.getLiveKycStatus(any())).thenReturn("APPROVED");
        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        accountService.getOrCreateAccount(sender);
        String recipientAccountNumber = accountService.getOrCreateAccount(recipient).getAccountNumber();
        creditWallet(sender, Currency.MYR, new BigDecimal("100.0000"));

        TransferResponse response = transferService.transfer(sender, "token",
                new TransferRequest(new RecipientDto(RecipientType.ACCOUNT_NUMBER, recipientAccountNumber),
                        Currency.MYR, Currency.MYR, new BigDecimal("40.0000"), null),
                "idem-" + UUID.randomUUID());

        assertThat(response.sourceAmount()).isEqualByComparingTo("40.0000");
        assertThat(response.destAmount()).isEqualByComparingTo("40.0000");
        assertThat(response.fxRate()).isNull();
        assertThat(walletFor(sender, Currency.MYR).getBalance()).isEqualByComparingTo("60.0000");
        assertThat(walletFor(recipient, Currency.MYR).getBalance()).isEqualByComparingTo("40.0000");
    }

    @Test
    void crossCurrencyTransferUsesClearingWalletsAndFxRate() {
        when(authServiceClient.getLiveKycStatus(any())).thenReturn("APPROVED");
        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        accountService.getOrCreateAccount(sender);
        String recipientAccountNumber = accountService.getOrCreateAccount(recipient).getAccountNumber();
        creditWallet(sender, Currency.MYR, new BigDecimal("100.0000"));

        TransferResponse response = transferService.transfer(sender, "token",
                new TransferRequest(new RecipientDto(RecipientType.ACCOUNT_NUMBER, recipientAccountNumber),
                        Currency.MYR, Currency.USD, new BigDecimal("100.0000"), null),
                "idem-" + UUID.randomUUID());

        assertThat(response.fxRate()).isNotNull();
        assertThat(response.destAmount()).isEqualByComparingTo(response.sourceAmount().multiply(response.fxRate())
                .setScale(4, java.math.RoundingMode.HALF_EVEN));
        assertThat(walletFor(sender, Currency.MYR).getBalance()).isEqualByComparingTo("0.0000");
        assertThat(walletFor(recipient, Currency.USD).getBalance()).isEqualByComparingTo(response.destAmount());
    }

    @Test
    void insufficientBalanceIsRejectedWithNoStateChange() {
        when(authServiceClient.getLiveKycStatus(any())).thenReturn("APPROVED");
        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        accountService.getOrCreateAccount(sender);
        String recipientAccountNumber = accountService.getOrCreateAccount(recipient).getAccountNumber();

        assertThatThrownBy(() -> transferService.transfer(sender, "token",
                new TransferRequest(new RecipientDto(RecipientType.ACCOUNT_NUMBER, recipientAccountNumber),
                        Currency.MYR, Currency.MYR, new BigDecimal("50.0000"), null),
                "idem-" + UUID.randomUUID()))
                .isInstanceOf(InsufficientBalanceException.class);
        assertThat(walletFor(sender, Currency.MYR).getBalance()).isEqualByComparingTo("0.0000");
    }

    @Test
    void selfTransferIsRejected() {
        when(authServiceClient.getLiveKycStatus(any())).thenReturn("APPROVED");
        UUID customer = UUID.randomUUID();
        String accountNumber = accountService.getOrCreateAccount(customer).getAccountNumber();
        creditWallet(customer, Currency.MYR, new BigDecimal("100.0000"));

        assertThatThrownBy(() -> transferService.transfer(customer, "token",
                new TransferRequest(new RecipientDto(RecipientType.ACCOUNT_NUMBER, accountNumber),
                        Currency.MYR, Currency.MYR, new BigDecimal("10.0000"), null),
                "idem-" + UUID.randomUUID()))
                .isInstanceOf(SelfTransferException.class);
    }

    @Test
    void nonApprovedKycBlocksTheTransfer() {
        when(authServiceClient.getLiveKycStatus(any())).thenReturn("PENDING");
        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        accountService.getOrCreateAccount(sender);
        String recipientAccountNumber = accountService.getOrCreateAccount(recipient).getAccountNumber();

        assertThatThrownBy(() -> transferService.transfer(sender, "token",
                new TransferRequest(new RecipientDto(RecipientType.ACCOUNT_NUMBER, recipientAccountNumber),
                        Currency.MYR, Currency.MYR, new BigDecimal("10.0000"), null),
                "idem-" + UUID.randomUUID()))
                .isInstanceOf(KycNotApprovedException.class);
    }

    @Test
    void concurrentTransfersFromTheSameWalletDoNotLoseUpdates() throws Exception {
        when(authServiceClient.getLiveKycStatus(any())).thenReturn("APPROVED");
        UUID sender = UUID.randomUUID();
        UUID recipientA = UUID.randomUUID();
        UUID recipientB = UUID.randomUUID();
        accountService.getOrCreateAccount(sender);
        String accountNumberA = accountService.getOrCreateAccount(recipientA).getAccountNumber();
        String accountNumberB = accountService.getOrCreateAccount(recipientB).getAccountNumber();
        creditWallet(sender, Currency.MYR, new BigDecimal("100.0000"));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> f1 = executor.submit(() -> transferService.transfer(sender, "token",
                new TransferRequest(new RecipientDto(RecipientType.ACCOUNT_NUMBER, accountNumberA),
                        Currency.MYR, Currency.MYR, new BigDecimal("60.0000"), null),
                "idem-" + UUID.randomUUID()));
        Future<?> f2 = executor.submit(() -> transferService.transfer(sender, "token",
                new TransferRequest(new RecipientDto(RecipientType.ACCOUNT_NUMBER, accountNumberB),
                        Currency.MYR, Currency.MYR, new BigDecimal("60.0000"), null),
                "idem-" + UUID.randomUUID()));

        int succeeded = 0;
        int failed = 0;
        for (Future<?> f : List.of(f1, f2)) {
            try {
                f.get();
                succeeded++;
            } catch (Exception e) {
                failed++;
            }
        }
        executor.shutdown();

        // Only one of the two 60.0000 transfers can succeed against a 100.0000 balance —
        // pessimistic locking must serialize them rather than letting both read a stale balance.
        assertThat(succeeded).isEqualTo(1);
        assertThat(failed).isEqualTo(1);
        assertThat(walletFor(sender, Currency.MYR).getBalance()).isEqualByComparingTo("40.0000");
    }

    private void creditWallet(UUID customerId, Currency currency, BigDecimal amount) {
        var wallet = walletFor(customerId, currency);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
    }

    private com.manekpay.ledger.entity.Wallet walletFor(UUID customerId, Currency currency) {
        var account = accountService.getOrCreateAccount(customerId);
        return walletRepository.findByAccountIdAndCurrency(account.getId(), currency).orElseThrow();
    }
}
