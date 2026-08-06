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
    public void applyRoundUp(TransactionCreatedEvent event) {
        if (event.currency() != event.homeCurrency()) {
            return;
        }
        BigDecimal roundUp = event.amount().setScale(0, RoundingMode.CEILING).subtract(event.amount());
        if (roundUp.signum() == 0) {
            return;
        }
        Vault vault = vaultRepository.findByCustomerId(event.customerId())
                .orElseGet(() -> vaultRepository.save(new Vault(event.customerId(), event.homeCurrency())));
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
