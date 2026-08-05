package com.manekpay.ledger.service;

import com.manekpay.ledger.entity.Account;
import com.manekpay.ledger.entity.Currency;
import com.manekpay.ledger.entity.Wallet;
import com.manekpay.ledger.repository.AccountRepository;
import com.manekpay.ledger.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.UUID;

@Service
public class AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final WalletRepository walletRepository;

    public AccountService(AccountRepository accountRepository, WalletRepository walletRepository) {
        this.accountRepository = accountRepository;
        this.walletRepository = walletRepository;
    }

    @Transactional
    public Account getOrCreateAccount(UUID customerId) {
        return accountRepository.findByCustomerId(customerId)
                .orElseGet(() -> createAccount(customerId));
    }

    private Account createAccount(UUID customerId) {
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
