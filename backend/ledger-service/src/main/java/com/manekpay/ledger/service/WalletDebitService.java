package com.manekpay.ledger.service;

import com.manekpay.ledger.dto.WalletDebitRequest;
import com.manekpay.ledger.dto.WalletDebitResponse;
import com.manekpay.ledger.entity.Account;
import com.manekpay.ledger.entity.VaultSweepDebit;
import com.manekpay.ledger.entity.Wallet;
import com.manekpay.ledger.exception.InsufficientBalanceException;
import com.manekpay.ledger.repository.VaultSweepDebitRepository;
import com.manekpay.ledger.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class WalletDebitService {

    private final AccountService accountService;
    private final WalletRepository walletRepository;
    private final VaultSweepDebitRepository vaultSweepDebitRepository;

    public WalletDebitService(AccountService accountService, WalletRepository walletRepository,
                               VaultSweepDebitRepository vaultSweepDebitRepository) {
        this.accountService = accountService;
        this.walletRepository = walletRepository;
        this.vaultSweepDebitRepository = vaultSweepDebitRepository;
    }

    // `reference` is the caller's idempotency key (goalId@nextSweepAt from vaults-service) - it
    // is stored on the audit row itself so this debit stays traceable independent of the 24h
    // Redis idempotency cache the IdempotencyFilter also relies on for request replay.
    @Transactional
    public WalletDebitResponse debit(WalletDebitRequest request, String reference) {
        // A prior attempt may have committed the debit and this audit row but lost its HTTP
        // response before the 24h Redis idempotency cache got populated (see WalletDebitService
        // finding #3 in the goal-vaults-recurring-sweeps review). Replaying with the same
        // reference must not move money twice - return the outcome already on record instead.
        Optional<VaultSweepDebit> existing = vaultSweepDebitRepository.findByReference(reference);
        if (existing.isPresent()) {
            return new WalletDebitResponse(existing.get().getBalanceAfter());
        }

        Account account = accountService.getOrCreateAccount(request.customerId());
        Wallet walletRef = walletRepository.findByAccountIdAndCurrency(account.getId(), request.currency())
                .orElseThrow(() -> new IllegalStateException("Missing wallet for " + request.currency()));
        Wallet wallet = walletRepository.findByIdForUpdate(walletRef.getId())
                .orElseThrow(() -> new IllegalStateException("Wallet disappeared mid-debit: " + walletRef.getId()));

        if (wallet.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException();
        }

        wallet.setBalance(wallet.getBalance().subtract(request.amount()));
        walletRepository.save(wallet);
        vaultSweepDebitRepository.save(new VaultSweepDebit(wallet.getId(), request.customerId(), request.amount(),
                request.currency(), wallet.getBalance(), reference));

        return new WalletDebitResponse(wallet.getBalance());
    }
}
