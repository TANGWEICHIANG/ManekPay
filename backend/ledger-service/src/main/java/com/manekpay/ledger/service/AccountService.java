package com.manekpay.ledger.service;

import com.manekpay.ledger.entity.Account;
import com.manekpay.ledger.repository.AccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountProvisioner accountProvisioner;

    public AccountService(AccountRepository accountRepository, AccountProvisioner accountProvisioner) {
        this.accountRepository = accountRepository;
        this.accountProvisioner = accountProvisioner;
    }

    public Account getOrCreateAccount(UUID customerId) {
        return accountRepository.findByCustomerId(customerId)
                .orElseGet(() -> createOrFetchExisting(customerId));
    }

    private Account createOrFetchExisting(UUID customerId) {
        try {
            return accountProvisioner.createAccount(customerId);
        } catch (DataIntegrityViolationException e) {
            // Lost the race: a concurrent request already created this customer's account
            // (unique(customer_id), V1 migration) between our check and our insert - this is
            // expected under the frontend's actual behavior (LedgerPage fires useMyAccount and
            // useTransfers in parallel on a brand-new customer's first load), not an error case.
            return accountRepository.findByCustomerId(customerId).orElseThrow(() -> e);
        }
    }
}
