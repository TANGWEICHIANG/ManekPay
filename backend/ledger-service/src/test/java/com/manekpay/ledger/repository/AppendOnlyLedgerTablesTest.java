package com.manekpay.ledger.repository;

import com.manekpay.ledger.entity.Account;
import com.manekpay.ledger.entity.Currency;
import com.manekpay.ledger.entity.Direction;
import com.manekpay.ledger.entity.LedgerEntry;
import com.manekpay.ledger.entity.Transfer;
import com.manekpay.ledger.entity.VaultSweepDebit;
import com.manekpay.ledger.entity.Wallet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

// V9 enforces append-only-ness at the database level (BEFORE UPDATE/DELETE triggers) as a second,
// independent layer beneath the repository-level restriction (TransferRepository/
// LedgerEntryRepository/VaultSweepDebitRepository no longer expose delete/update methods at all -
// see those interfaces). This test proves the database layer holds even when bypassing the
// repositories entirely via raw SQL, the way a future bug or a manual DB session could.
@SpringBootTest
@Testcontainers
class AppendOnlyLedgerTablesTest {

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
    private TransferRepository transferRepository;
    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;
    @Autowired
    private VaultSweepDebitRepository vaultSweepDebitRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void ldg04TransfersRejectsUpdateAndDelete() {
        Account account = accountRepository.save(new Account(UUID.randomUUID(), accountNumber()));
        Wallet fromWallet = walletRepository.save(new Wallet(account.getId(), Currency.MYR));
        Wallet toWallet = walletRepository.save(new Wallet(account.getId(), Currency.USD));
        Transfer transfer = transferRepository.save(new Transfer(fromWallet.getId(), toWallet.getId(),
                new BigDecimal("10.0000"), Currency.MYR, new BigDecimal("2.2000"), Currency.USD,
                new BigDecimal("0.22000000"), "append-only-test-" + UUID.randomUUID(), null, null, null));

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE ldg04_transfers SET fx_rate = fx_rate WHERE id = ?", transfer.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM ldg04_transfers WHERE id = ?", transfer.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
    }

    @Test
    void ldg05LedgerEntriesRejectsUpdateAndDelete() {
        Account account = accountRepository.save(new Account(UUID.randomUUID(), accountNumber()));
        Wallet fromWallet = walletRepository.save(new Wallet(account.getId(), Currency.MYR));
        Wallet toWallet = walletRepository.save(new Wallet(account.getId(), Currency.USD));
        Transfer transfer = transferRepository.save(new Transfer(fromWallet.getId(), toWallet.getId(),
                new BigDecimal("10.0000"), Currency.MYR, new BigDecimal("2.2000"), Currency.USD,
                new BigDecimal("0.22000000"), "append-only-test-" + UUID.randomUUID(), null, null, null));
        LedgerEntry entry = ledgerEntryRepository.save(new LedgerEntry(transfer.getId(), fromWallet.getId(),
                Direction.DEBIT, new BigDecimal("10.0000"), Currency.MYR, new BigDecimal("-10.0000")));

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE ldg05_ledger_entries SET amount = amount WHERE id = ?", entry.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM ldg05_ledger_entries WHERE id = ?", entry.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
    }

    @Test
    void ldg06VaultSweepDebitsRejectsUpdateAndDelete() {
        VaultSweepDebit debit = vaultSweepDebitRepository.save(new VaultSweepDebit(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("5.0000"), Currency.MYR, new BigDecimal("95.0000"), "append-only-test-" + UUID.randomUUID()));

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE ldg06_vault_sweep_debits SET amount = amount WHERE id = ?", debit.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM ldg06_vault_sweep_debits WHERE id = ?", debit.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
    }

    // account_number is VARCHAR(20) UNIQUE - a random 12-digit numeric string stays well within
    // that and avoids collisions across repeated runs against the same database.
    private static String accountNumber() {
        return String.valueOf(100000000000L + Math.abs(UUID.randomUUID().getLeastSignificantBits() % 900000000000L));
    }
}
