package com.manekpay.ledger.repository;

import com.manekpay.ledger.entity.Account;
import com.manekpay.ledger.entity.AccountProxy;
import com.manekpay.ledger.entity.Currency;
import com.manekpay.ledger.entity.Direction;
import com.manekpay.ledger.entity.LedgerEntry;
import com.manekpay.ledger.entity.ProxyType;
import com.manekpay.ledger.entity.Transfer;
import com.manekpay.ledger.entity.Wallet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class RepositorySmokeTest {

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
    private AccountRepository accountRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private AccountProxyRepository accountProxyRepository;
    @Autowired
    private TransferRepository transferRepository;
    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Test
    @WithMockUser
    @Transactional
    // findByIdForUpdate (below) issues a PESSIMISTIC_WRITE query, which JPA requires an active
    // transaction to run - without this, each repository call above runs in its own short-lived
    // transaction that's already closed by the time that line executes. This also rolls the whole
    // test back at the end, so it no longer leaves rows behind for a rerun to collide with.
    void savesAndLoadsAccountWalletsProxyTransferAndLedgerEntries() {
        Account account = accountRepository.save(new Account(UUID.randomUUID(), "100000000001"));
        assertThat(account.getId()).isNotNull();

        Wallet myrWallet = walletRepository.save(new Wallet(account.getId(), Currency.MYR));
        Wallet usdWallet = walletRepository.save(new Wallet(account.getId(), Currency.USD));
        assertThat(walletRepository.findByAccountId(account.getId())).hasSize(2);
        assertThat(walletRepository.findByAccountIdAndCurrency(account.getId(), Currency.MYR)).isPresent();

        List<Wallet> clearingWallets = walletRepository.findAll().stream()
                .filter(w -> w.getAccountId() == null)
                .toList();
        assertThat(clearingWallets).hasSize(5);
        assertThat(walletRepository.findByAccountIdIsNullAndCurrency(Currency.MYR)).isPresent();

        AccountProxy proxy = accountProxyRepository.save(new AccountProxy(account.getId(), ProxyType.MOBILE, "0123456789"));
        // Neither AccountProxy nor LedgerEntry (below) override equals()/hashCode() - compare on
        // id, the property that actually identifies "the same row", rather than depending on
        // object identity, which was the whole bug before @Transactional made save() and find()
        // share one persistence context here.
        assertThat(accountProxyRepository.findByTypeAndValue(ProxyType.MOBILE, "0123456789").map(AccountProxy::getId))
                .contains(proxy.getId());

        Transfer transfer = transferRepository.save(new Transfer(myrWallet.getId(), usdWallet.getId(),
                new BigDecimal("100.0000"), Currency.MYR, new BigDecimal("22.0000"), Currency.USD,
                new BigDecimal("0.22000000"), "idem-key-1", null, null, null));
        assertThat(transfer.getId()).isNotNull();

        LedgerEntry debitEntry = ledgerEntryRepository.save(new LedgerEntry(transfer.getId(), myrWallet.getId(),
                Direction.DEBIT, new BigDecimal("100.0000"), Currency.MYR, new BigDecimal("-100.0000")));
        assertThat(ledgerEntryRepository.findByWalletId(myrWallet.getId()))
                .extracting(LedgerEntry::getId)
                .containsExactly(debitEntry.getId());

        Optional<Wallet> lockedWallet = walletRepository.findByIdForUpdate(myrWallet.getId());
        assertThat(lockedWallet).isPresent();
    }
}
