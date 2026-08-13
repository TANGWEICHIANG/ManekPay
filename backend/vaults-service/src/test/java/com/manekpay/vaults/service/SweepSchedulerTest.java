package com.manekpay.vaults.service;

import com.manekpay.vaults.entity.Currency;
import com.manekpay.vaults.entity.SweepFrequency;
import com.manekpay.vaults.entity.Vault;
import com.manekpay.vaults.exception.InsufficientBalanceException;
import com.manekpay.vaults.exception.LedgerServiceUnavailableException;
import com.manekpay.vaults.repository.VaultRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class SweepSchedulerTest {

    @Mock
    private VaultRepository vaultRepository;
    @Mock
    private LedgerServiceClient ledgerServiceClient;
    @InjectMocks
    private SweepScheduler scheduler;

    private Vault dueGoal() {
        Vault goal = new Vault(UUID.randomUUID(), "Emergency Fund", Currency.MYR, new BigDecimal("5000.00"),
                new BigDecimal("50.00"), SweepFrequency.WEEKLY);
        goal.setNextSweepAt(Instant.parse("2026-01-01T00:00:00Z"));
        return goal;
    }

    @Test
    void successfulSweepCreditsTheVaultAndAdvancesNextSweepAt() {
        Vault goal = dueGoal();

        scheduler.runSweep(goal);

        verify(ledgerServiceClient).debitWallet(eq(goal.getCustomerId()), eq(Currency.MYR),
                eq(new BigDecimal("50.00")), anyString());
        assertThat(goal.getBalance()).isEqualByComparingTo("50.00");
        assertThat(goal.getNextSweepAt()).isEqualTo(Instant.parse("2026-01-08T00:00:00Z"));
        assertThat(goal.getLastSweepAt()).isNotNull();
        verify(vaultRepository).save(goal);
    }

    @Test
    void insufficientBalanceStillAdvancesNextSweepAtButDoesNotCreditTheVault() {
        Vault goal = dueGoal();
        doThrow(new InsufficientBalanceException()).when(ledgerServiceClient)
                .debitWallet(any(), any(), any(), anyString());

        scheduler.runSweep(goal);

        assertThat(goal.getBalance()).isEqualByComparingTo("0.00");
        assertThat(goal.getNextSweepAt()).isEqualTo(Instant.parse("2026-01-08T00:00:00Z"));
        verify(vaultRepository).save(goal);
    }

    @Test
    void transientFailureLeavesNextSweepAtUnchangedForARetryOnTheNextTick() {
        Vault goal = dueGoal();
        Instant originalNextSweepAt = goal.getNextSweepAt();
        doThrow(new LedgerServiceUnavailableException(new RuntimeException("connection refused")))
                .when(ledgerServiceClient).debitWallet(any(), any(), any(), anyString());

        scheduler.runSweep(goal);

        assertThat(goal.getBalance()).isEqualByComparingTo("0.00");
        assertThat(goal.getNextSweepAt()).isEqualTo(originalNextSweepAt);
        verifyNoMoreInteractions(vaultRepository);
    }
}
