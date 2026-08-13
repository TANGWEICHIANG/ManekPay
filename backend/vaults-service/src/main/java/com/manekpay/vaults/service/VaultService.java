package com.manekpay.vaults.service;

import com.manekpay.vaults.dto.TransactionCreatedEvent;
import com.manekpay.vaults.entity.RoundUp;
import com.manekpay.vaults.entity.Vault;
import com.manekpay.vaults.repository.RoundUpRepository;
import com.manekpay.vaults.repository.VaultRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class VaultService {

    private static final Logger log = LoggerFactory.getLogger(VaultService.class);

    private final VaultRepository vaultRepository;
    private final RoundUpRepository roundUpRepository;

    public VaultService(VaultRepository vaultRepository, RoundUpRepository roundUpRepository) {
        this.vaultRepository = vaultRepository;
        this.roundUpRepository = roundUpRepository;
    }

    // No extra locking needed: TransactionEventPublisher (ledger-service) keys every message by
    // customerId, so Kafka guarantees one customer's events land on the same partition and are
    // consumed strictly in order by a single thread. The unique(transaction_id) constraint below
    // only needs to guard against *sequential* redelivery after a consumer crash/rebalance.
    //
    // @Transactional so the dedup insert and the balance update commit together - otherwise a
    // crash between the two would leave the dedup row committed but the balance never credited,
    // silently and permanently dropping that round-up (redelivery would just hit the dedup guard
    // and skip, since it can't tell "already recorded" apart from "recorded but not credited").
    @Transactional
    public void applyRoundUp(TransactionCreatedEvent event) {
        if (event.currency() != event.homeCurrency()) {
            return;
        }
        BigDecimal roundUp = event.amount().setScale(0, RoundingMode.CEILING).subtract(event.amount());
        if (roundUp.signum() == 0) {
            return;
        }
        Vault vault = vaultRepository.findByCustomerIdAndNameIsNull(event.customerId())
                .orElseGet(() -> vaultRepository.save(new Vault(event.customerId(), event.homeCurrency())));
        // A customer's resolved home currency can change between transfers (e.g. they resubmit
        // KYC with a different declared nationality) - once a vault exists in a currency, later
        // events in a different currency are skipped rather than silently mixed into its balance.
        if (vault.getCurrency() != event.homeCurrency()) {
            log.debug("Skipping round-up for transaction {}: vault currency {} no longer matches home currency {}",
                    event.transactionId(), vault.getCurrency(), event.homeCurrency());
            return;
        }
        try {
            roundUpRepository.save(new RoundUp(vault.getId(), event.transactionId(), roundUp));
        } catch (DataIntegrityViolationException e) {
            log.debug("Round-up for transaction {} already recorded, skipping redelivery", event.transactionId());
            return;
        }
        vault.setBalance(vault.getBalance().add(roundUp));
        vaultRepository.save(vault);
    }
}
