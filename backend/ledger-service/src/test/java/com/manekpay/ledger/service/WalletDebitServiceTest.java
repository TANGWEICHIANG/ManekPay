package com.manekpay.ledger.service;

import com.manekpay.ledger.dto.WalletDebitRequest;
import com.manekpay.ledger.entity.Currency;
import com.manekpay.ledger.entity.VaultSweepDebit;
import com.manekpay.ledger.repository.VaultSweepDebitRepository;
import com.manekpay.ledger.repository.WalletRepository;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Unit-level (no Spring context, no DB) so it actually runs in environments where the
// Testcontainers-based WalletDebitControllerIntegrationTest cannot (Docker unreachable).
class WalletDebitServiceTest {

    @Test
    void replaysAStrandedDebitInsteadOfDebitingAgainWhenAVaultSweepDebitAlreadyExistsForTheReference() {
        AccountService accountService = mock(AccountService.class);
        WalletRepository walletRepository = mock(WalletRepository.class);
        VaultSweepDebitRepository vaultSweepDebitRepository = mock(VaultSweepDebitRepository.class);
        WalletDebitService service = new WalletDebitService(accountService, walletRepository, vaultSweepDebitRepository);

        String reference = UUID.randomUUID() + "@" + java.time.Instant.now();
        VaultSweepDebit existing = new VaultSweepDebit(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("25.0000"), Currency.MYR, new BigDecimal("75.0000"), reference);
        when(vaultSweepDebitRepository.findByReference(reference)).thenReturn(Optional.of(existing));

        WalletDebitRequest request = new WalletDebitRequest(UUID.randomUUID(), Currency.MYR, new BigDecimal("25.0000"));
        var response = service.debit(request, reference);

        assertThat(response.balance()).isEqualByComparingTo("75.0000");
        // No wallet was touched and no new audit row was inserted - this was a safe replay, not a
        // second debit.
        verifyNoInteractions(accountService, walletRepository);
    }
}
