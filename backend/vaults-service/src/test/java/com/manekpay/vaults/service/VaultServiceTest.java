package com.manekpay.vaults.service;

import com.manekpay.vaults.dto.TransactionCreatedEvent;
import com.manekpay.vaults.entity.Currency;
import com.manekpay.vaults.entity.RoundUp;
import com.manekpay.vaults.entity.Vault;
import com.manekpay.vaults.repository.RoundUpRepository;
import com.manekpay.vaults.repository.VaultRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VaultServiceTest {

    @Mock
    private VaultRepository vaultRepository;
    @Mock
    private RoundUpRepository roundUpRepository;

    @Test
    void roundsUpToNearestWholeUnitAndAddsToExistingVaultBalance() {
        VaultService service = new VaultService(vaultRepository, roundUpRepository);
        UUID customerId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        Vault vault = new Vault(customerId, Currency.MYR);
        vault.setBalance(new BigDecimal("5.0000"));
        when(vaultRepository.findByCustomerIdAndNameIsNull(customerId)).thenReturn(Optional.of(vault));

        TransactionCreatedEvent event = new TransactionCreatedEvent(
                transactionId, customerId, new BigDecimal("12.3000"), Currency.MYR, Currency.MYR, Instant.now());

        service.applyRoundUp(event);

        ArgumentCaptor<RoundUp> roundUpCaptor = ArgumentCaptor.forClass(RoundUp.class);
        verify(roundUpRepository).save(roundUpCaptor.capture());
        assertThat(roundUpCaptor.getValue().getAmount()).isEqualByComparingTo("0.7000");
        assertThat(roundUpCaptor.getValue().getTransactionId()).isEqualTo(transactionId);

        assertThat(vault.getBalance()).isEqualByComparingTo("5.7000");
        verify(vaultRepository).save(vault);
    }

    @Test
    void createsANewVaultOnACustomersFirstMatchingCurrencyTransfer() {
        VaultService service = new VaultService(vaultRepository, roundUpRepository);
        UUID customerId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        when(vaultRepository.findByCustomerIdAndNameIsNull(customerId)).thenReturn(Optional.empty());
        when(vaultRepository.save(any(Vault.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionCreatedEvent event = new TransactionCreatedEvent(
                transactionId, customerId, new BigDecimal("1.2500"), Currency.SGD, Currency.SGD, Instant.now());

        service.applyRoundUp(event);

        ArgumentCaptor<Vault> vaultCaptor = ArgumentCaptor.forClass(Vault.class);
        verify(vaultRepository, times(2)).save(vaultCaptor.capture());
        Vault createdVault = vaultCaptor.getAllValues().get(0);
        assertThat(createdVault.getCurrency()).isEqualTo(Currency.SGD);
        assertThat(vaultCaptor.getAllValues().get(1).getBalance()).isEqualByComparingTo("0.7500");
    }

    @Test
    void skipsWhenTransferCurrencyDoesNotMatchHomeCurrency() {
        VaultService service = new VaultService(vaultRepository, roundUpRepository);
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("12.3000"), Currency.USD, Currency.MYR, Instant.now());

        service.applyRoundUp(event);

        verify(vaultRepository, never()).findByCustomerIdAndNameIsNull(any());
        verify(roundUpRepository, never()).save(any());
    }

    @Test
    void skipsWhenAmountIsAlreadyAWholeNumber() {
        VaultService service = new VaultService(vaultRepository, roundUpRepository);
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("10.0000"), Currency.MYR, Currency.MYR, Instant.now());

        service.applyRoundUp(event);

        verify(vaultRepository, never()).findByCustomerIdAndNameIsNull(any());
        verify(roundUpRepository, never()).save(any());
    }

    @Test
    void skipsWithoutTouchingBalanceWhenVaultCurrencyNoLongerMatchesHomeCurrency() {
        VaultService service = new VaultService(vaultRepository, roundUpRepository);
        UUID customerId = UUID.randomUUID();
        Vault vault = new Vault(customerId, Currency.MYR);
        vault.setBalance(new BigDecimal("5.0000"));
        when(vaultRepository.findByCustomerIdAndNameIsNull(customerId)).thenReturn(Optional.of(vault));

        TransactionCreatedEvent event = new TransactionCreatedEvent(
                UUID.randomUUID(), customerId, new BigDecimal("12.3000"), Currency.SGD, Currency.SGD, Instant.now());

        service.applyRoundUp(event);

        assertThat(vault.getBalance()).isEqualByComparingTo("5.0000");
        verify(roundUpRepository, never()).save(any());
        verify(vaultRepository, never()).save(vault);
    }

    @Test
    void skipsWithoutTouchingBalanceWhenRoundUpAlreadyRecorded() {
        VaultService service = new VaultService(vaultRepository, roundUpRepository);
        UUID customerId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        Vault vault = new Vault(customerId, Currency.MYR);
        vault.setBalance(new BigDecimal("5.0000"));
        when(vaultRepository.findByCustomerIdAndNameIsNull(customerId)).thenReturn(Optional.of(vault));
        when(roundUpRepository.save(any(RoundUp.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate transaction_id"));

        TransactionCreatedEvent event = new TransactionCreatedEvent(
                transactionId, customerId, new BigDecimal("12.3000"), Currency.MYR, Currency.MYR, Instant.now());

        service.applyRoundUp(event);

        assertThat(vault.getBalance()).isEqualByComparingTo("5.0000");
        verify(vaultRepository, never()).save(vault);
    }
}
