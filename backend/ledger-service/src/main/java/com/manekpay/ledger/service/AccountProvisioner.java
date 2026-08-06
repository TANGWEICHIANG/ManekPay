package com.manekpay.ledger.service;

import com.manekpay.ledger.entity.Account;
import com.manekpay.ledger.entity.Currency;
import com.manekpay.ledger.entity.Wallet;
import com.manekpay.ledger.repository.AccountRepository;
import com.manekpay.ledger.repository.WalletRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.UUID;

// Split out of AccountService so the REQUIRES_NEW transaction below actually goes through
// Spring's proxy - a self-invoked @Transactional method on the same bean is not proxied and
// would silently run in the caller's existing transaction instead.
@Component
public class AccountProvisioner {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final WalletRepository walletRepository;

    public AccountProvisioner(AccountRepository accountRepository, WalletRepository walletRepository) {
        this.accountRepository = accountRepository;
        this.walletRepository = walletRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Account createAccount(UUID customerId) {
        Account account = accountRepository.save(new Account(customerId, generateAccountNumber()));
        for (Currency currency : Currency.values()) {
            walletRepository.save(new Wallet(account.getId(), currency));
        }
        return account;
    }

    private String generateAccountNumber() {
        String candidate;
        do {
            candidate = String.format("%012d", (long) (RANDOM.nextDouble() * 1_000_000_000_000L));
        } while (accountRepository.findByAccountNumber(candidate).isPresent());
        return candidate;
    }
}
