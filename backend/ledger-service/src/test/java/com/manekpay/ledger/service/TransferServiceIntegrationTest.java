package com.manekpay.ledger.service;

import com.manekpay.ledger.dto.RecipientDto;
import com.manekpay.ledger.dto.RecipientType;
import com.manekpay.ledger.dto.TransferRequest;
import com.manekpay.ledger.dto.TransferResponse;
import com.manekpay.ledger.entity.Currency;
import com.manekpay.ledger.entity.Direction;
import com.manekpay.ledger.entity.LedgerEntry;
import com.manekpay.ledger.exception.InsufficientBalanceException;
import com.manekpay.ledger.exception.KycNotApprovedException;
import com.manekpay.ledger.exception.SelfTransferException;
import com.manekpay.ledger.repository.LedgerEntryRepository;
import com.manekpay.ledger.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;
    @MockBean
    private AuthServiceClient authServiceClient;
    @MockBean
    private FxRateProvider fxRateProvider;

    // The real FxRateProvider (FxServiceClient) makes a live HTTP call to fx-service, which isn't
    // reachable in this test environment - every test that touches any currency conversion
    // (cross-currency transfers, top-ups) needs a mocked rate, or it throws
    // FxServiceUnavailableException instead of testing anything. This default covers tests that
    // only need *a* non-null rate; tests that need a specific rate for their own arithmetic
    // override it with a more specific stub, which Mockito prefers over this catch-all.
    @BeforeEach
    void stubDefaultFxRate() {
        when(fxRateProvider.getRate(any(), any(), any())).thenReturn(new BigDecimal("2.0000"));
    }

    @Test
    void sameCurrencyTransferMovesBalanceWithTwoLedgerEntries() {
        when(authServiceClient.getLiveKycStatus(any())).thenReturn("APPROVED");
        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        accountService.getOrCreateAccount(sender);
        String recipientAccountNumber = accountService.getOrCreateAccount(recipient).getAccountNumber();
        creditWallet(sender, Currency.MYR, new BigDecimal("100.0000"));

        TransferResponse response = transferService.transfer(sender, "token", Currency.MYR,
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

        TransferResponse response = transferService.transfer(sender, "token", Currency.MYR,
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

        assertThatThrownBy(() -> transferService.transfer(sender, "token", Currency.MYR,
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

        assertThatThrownBy(() -> transferService.transfer(customer, "token", Currency.MYR,
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

        assertThatThrownBy(() -> transferService.transfer(sender, "token", Currency.MYR,
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
        Future<?> f1 = executor.submit(() -> transferService.transfer(sender, "token", Currency.MYR,
                new TransferRequest(new RecipientDto(RecipientType.ACCOUNT_NUMBER, accountNumberA),
                        Currency.MYR, Currency.MYR, new BigDecimal("60.0000"), null),
                "idem-" + UUID.randomUUID()));
        Future<?> f2 = executor.submit(() -> transferService.transfer(sender, "token", Currency.MYR,
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

    @Test
    void shortfallOnlyIsToppedUpFromTheHomeCurrencyWalletPreservingExistingSourceBalance() {
        when(authServiceClient.getLiveKycStatus(any())).thenReturn("APPROVED");
        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        accountService.getOrCreateAccount(sender);
        String recipientAccountNumber = accountService.getOrCreateAccount(recipient).getAccountNumber();
        creditWallet(sender, Currency.SGD, new BigDecimal("30.0000"));
        creditWallet(sender, Currency.MYR, new BigDecimal("500.0000"));

        TransferResponse response = transferService.transfer(sender, "token", Currency.MYR,
                new TransferRequest(new RecipientDto(RecipientType.ACCOUNT_NUMBER, recipientAccountNumber),
                        Currency.SGD, Currency.SGD, new BigDecimal("100.0000"), null),
                "idem-" + UUID.randomUUID());

        assertThat(response.topUpAmount()).isNotNull();
        assertThat(response.topUpCurrency()).isEqualTo(Currency.MYR);
        assertThat(response.topUpFxRate()).isNotNull();
        assertThat(walletFor(sender, Currency.SGD).getBalance()).isEqualByComparingTo("0.0000");
        assertThat(walletFor(recipient, Currency.SGD).getBalance()).isEqualByComparingTo("100.0000");
        // The home wallet must be debited by exactly the 70.0000 SGD shortfall's worth (not the
        // full 100.0000 transfer amount) - computed from the actual rate the transfer used, since
        // FxServiceClient's rates jitter and can't be hardcoded. This is the assertion that
        // actually proves "existing balance used first" rather than merely checking the debit is
        // nonzero.
        BigDecimal expectedShortfallDebit = new BigDecimal("70.0000")
                .divide(response.topUpFxRate(), 4, java.math.RoundingMode.HALF_EVEN);
        assertThat(walletFor(sender, Currency.MYR).getBalance())
                .isEqualByComparingTo(new BigDecimal("500.0000").subtract(expectedShortfallDebit));
    }

    @Test
    void fullAmountIsToppedUpFromHomeCurrencyWhenSourceWalletIsEmpty() {
        when(authServiceClient.getLiveKycStatus(any())).thenReturn("APPROVED");
        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        accountService.getOrCreateAccount(sender);
        String recipientAccountNumber = accountService.getOrCreateAccount(recipient).getAccountNumber();
        creditWallet(sender, Currency.MYR, new BigDecimal("500.0000"));

        TransferResponse response = transferService.transfer(sender, "token", Currency.MYR,
                new TransferRequest(new RecipientDto(RecipientType.ACCOUNT_NUMBER, recipientAccountNumber),
                        Currency.SGD, Currency.SGD, new BigDecimal("50.0000"), null),
                "idem-" + UUID.randomUUID());

        assertThat(response.topUpAmount()).isNotNull();
        assertThat(walletFor(sender, Currency.SGD).getBalance()).isEqualByComparingTo("0.0000");
        assertThat(walletFor(recipient, Currency.SGD).getBalance()).isEqualByComparingTo("50.0000");
        // The home wallet must actually be debited by the full 50.0000 shortfall's worth (computed
        // from the actual rate used) - without this, a wrong top-up magnitude on the home side
        // would pass undetected since only the SGD-side balances were checked above.
        BigDecimal expectedHomeDebit = new BigDecimal("50.0000")
                .divide(response.topUpFxRate(), 4, RoundingMode.HALF_EVEN);
        assertThat(walletFor(sender, Currency.MYR).getBalance())
                .isEqualByComparingTo(new BigDecimal("500.0000").subtract(expectedHomeDebit));
    }

    @Test
    void noTopUpIsAttemptedWhenSourceCurrencyEqualsHomeCurrency() {
        when(authServiceClient.getLiveKycStatus(any())).thenReturn("APPROVED");
        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        accountService.getOrCreateAccount(sender);
        String recipientAccountNumber = accountService.getOrCreateAccount(recipient).getAccountNumber();

        assertThatThrownBy(() -> transferService.transfer(sender, "token", Currency.MYR,
                new TransferRequest(new RecipientDto(RecipientType.ACCOUNT_NUMBER, recipientAccountNumber),
                        Currency.MYR, Currency.MYR, new BigDecimal("10.0000"), null),
                "idem-" + UUID.randomUUID()))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    void transferFailsWhenHomeCurrencyWalletCannotCoverTheShortfallEither() {
        when(authServiceClient.getLiveKycStatus(any())).thenReturn("APPROVED");
        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        accountService.getOrCreateAccount(sender);
        String recipientAccountNumber = accountService.getOrCreateAccount(recipient).getAccountNumber();
        creditWallet(sender, Currency.SGD, new BigDecimal("10.0000"));
        // No MYR balance at all - the home-currency wallet exists (every account gets one per
        // currency) but has nothing in it.

        assertThatThrownBy(() -> transferService.transfer(sender, "token", Currency.MYR,
                new TransferRequest(new RecipientDto(RecipientType.ACCOUNT_NUMBER, recipientAccountNumber),
                        Currency.SGD, Currency.SGD, new BigDecimal("100.0000"), null),
                "idem-" + UUID.randomUUID()))
                .isInstanceOf(InsufficientBalanceException.class);
        // No partial state change - the SGD wallet must be untouched, matching the existing
        // insufficientBalanceIsRejectedWithNoStateChange test's own assertion style.
        assertThat(walletFor(sender, Currency.SGD).getBalance()).isEqualByComparingTo("10.0000");
    }

    @Test
    void sufficientBalanceNeverTriggersATopUpAndResponseFieldsStayNull() {
        when(authServiceClient.getLiveKycStatus(any())).thenReturn("APPROVED");
        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        accountService.getOrCreateAccount(sender);
        String recipientAccountNumber = accountService.getOrCreateAccount(recipient).getAccountNumber();
        creditWallet(sender, Currency.SGD, new BigDecimal("200.0000"));

        TransferResponse response = transferService.transfer(sender, "token", Currency.MYR,
                new TransferRequest(new RecipientDto(RecipientType.ACCOUNT_NUMBER, recipientAccountNumber),
                        Currency.SGD, Currency.SGD, new BigDecimal("50.0000"), null),
                "idem-" + UUID.randomUUID());

        assertThat(response.topUpAmount()).isNull();
        assertThat(response.topUpCurrency()).isNull();
        assertThat(response.topUpFxRate()).isNull();
        assertThat(walletFor(sender, Currency.SGD).getBalance()).isEqualByComparingTo("150.0000");
    }

    @Test
    void crossCurrencyTransferWithTopUpCorrectlySequencesBothLegsOnASharedClearingWallet() {
        when(authServiceClient.getLiveKycStatus(any())).thenReturn("APPROVED");
        // destCurrency == homeCurrency (both MYR) forces the home-clearing wallet and the
        // destination-clearing wallet to be the same row, mutated once by the top-up leg and
        // again by the cross-currency leg - the trickiest case in the design spec.
        when(fxRateProvider.getRate(eq(Currency.SGD), eq(Currency.MYR), any())).thenReturn(new BigDecimal("3.3000"));
        when(fxRateProvider.getRate(eq(Currency.MYR), eq(Currency.SGD), any())).thenReturn(new BigDecimal("0.3000"));
        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        accountService.getOrCreateAccount(sender);
        String recipientAccountNumber = accountService.getOrCreateAccount(recipient).getAccountNumber();
        creditWallet(sender, Currency.SGD, new BigDecimal("30.0000"));
        creditWallet(sender, Currency.MYR, new BigDecimal("1000.0000"));
        // The debit==credit assertions below hold for any pairing of entries to wallets - they
        // can't distinguish "the shared clearing wallet correctly compounded both legs" from "one
        // leg silently overwrote the other". Only an assertion on the shared wallet's own balance
        // (and the money that actually reached the recipient) can catch that regression, so this
        // captures the shared MYR clearing wallet's balance before the transfer to assert its
        // exact post-transfer delta below.
        BigDecimal myrClearingBefore = walletRepository.findByAccountIdIsNullAndCurrency(Currency.MYR)
                .orElseThrow().getBalance();

        TransferResponse response = transferService.transfer(sender, "token", Currency.MYR,
                new TransferRequest(new RecipientDto(RecipientType.ACCOUNT_NUMBER, recipientAccountNumber),
                        Currency.SGD, Currency.MYR, new BigDecimal("100.0000"), null),
                "idem-" + UUID.randomUUID());

        // Shortfall = 100.0000 - 30.0000 = 70.0000 SGD; at a 0.3000 MYR->SGD rate that's
        // 70.0000 / 0.3000 = 233.3333 MYR.
        assertThat(response.topUpAmount()).isEqualByComparingTo("233.3333");
        assertThat(response.topUpCurrency()).isEqualTo(Currency.MYR);
        assertThat(response.topUpFxRate()).isEqualByComparingTo("0.3000");

        List<LedgerEntry> entries = ledgerEntryRepository.findByTransferId(response.transferId());
        // 4 legs from the top-up (home debit, home-clearing credit, source-clearing debit, sender
        // credit) + 4 legs from the main transfer (sender debit, source-clearing credit,
        // dest-clearing debit, recipient credit) = 8.
        assertThat(entries).hasSize(8);

        Map<Currency, BigDecimal> debitTotals = entries.stream()
                .filter(e -> e.getDirection() == Direction.DEBIT)
                .collect(Collectors.groupingBy(LedgerEntry::getCurrency,
                        Collectors.reducing(BigDecimal.ZERO, LedgerEntry::getAmount, BigDecimal::add)));
        Map<Currency, BigDecimal> creditTotals = entries.stream()
                .filter(e -> e.getDirection() == Direction.CREDIT)
                .collect(Collectors.groupingBy(LedgerEntry::getCurrency,
                        Collectors.reducing(BigDecimal.ZERO, LedgerEntry::getAmount, BigDecimal::add)));

        // Double-entry balance per currency - necessary, but not sufficient on its own: every
        // amount here is emitted once as a DEBIT and once as a CREDIT by construction, so this
        // holds regardless of which wallet each leg actually mutated.
        assertThat(debitTotals.get(Currency.SGD)).isEqualByComparingTo(creditTotals.get(Currency.SGD));
        assertThat(debitTotals.get(Currency.MYR)).isEqualByComparingTo(creditTotals.get(Currency.MYR));

        // The assertion that actually catches a dedup/sequencing regression: the shared MYR
        // clearing wallet must reflect BOTH legs compounding on the same locked instance (the
        // top-up's +233.3333 credit, then the cross-currency leg's -330.0000 debit applied on top
        // of it) rather than one leg overwriting the other.
        assertThat(walletRepository.findByAccountIdIsNullAndCurrency(Currency.MYR).orElseThrow().getBalance())
                .isEqualByComparingTo(myrClearingBefore.add(new BigDecimal("233.3333")).subtract(new BigDecimal("330.0000")));
        // And that the recipient actually received the correctly-computed destAmount.
        assertThat(walletFor(recipient, Currency.MYR).getBalance()).isEqualByComparingTo("330.0000");
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
