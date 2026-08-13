package com.manekpay.vaults.service;

import com.manekpay.vaults.entity.Vault;
import com.manekpay.vaults.exception.InsufficientBalanceException;
import com.manekpay.vaults.exception.LedgerServiceUnavailableException;
import com.manekpay.vaults.repository.VaultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class SweepScheduler {

    private static final Logger log = LoggerFactory.getLogger(SweepScheduler.class);

    private final VaultRepository vaultRepository;
    private final LedgerServiceClient ledgerServiceClient;

    public SweepScheduler(VaultRepository vaultRepository, LedgerServiceClient ledgerServiceClient) {
        this.vaultRepository = vaultRepository;
        this.ledgerServiceClient = ledgerServiceClient;
    }

    @Scheduled(fixedRate = 3_600_000)
    public void runDueSweeps() {
        List<Vault> due = vaultRepository.findBySweepActiveTrueAndNextSweepAtLessThanEqual(Instant.now());
        for (Vault goal : due) {
            try {
                runSweep(goal);
            } catch (Exception e) {
                // One goal's unexpected failure must not abort the batch - every other due goal
                // still gets its chance this tick.
                log.error("Sweep failed unexpectedly for goal {}", goal.getId(), e);
            }
        }
    }

    void runSweep(Vault goal) {
        String idempotencyKey = goal.getId() + "@" + goal.getNextSweepAt();
        try {
            ledgerServiceClient.debitWallet(goal.getCustomerId(), goal.getCurrency(), goal.getSweepAmount(), idempotencyKey);
        } catch (InsufficientBalanceException e) {
            // A customer who's broke this cycle shouldn't be hammered every scheduler tick -
            // just retry next scheduled period instead of every tick until the tick after that.
            log.info("Skipping sweep for goal {}: insufficient balance", goal.getId());
            goal.setNextSweepAt(goal.getSweepFrequency().nextRunAfter(goal.getNextSweepAt()));
            vaultRepository.save(goal);
            return;
        } catch (LedgerServiceUnavailableException e) {
            // Network/5xx: leave nextSweepAt unchanged so the very next scheduler tick retries it.
            log.error("Sweep for goal {} could not reach ledger-service, will retry next tick", goal.getId(), e);
            return;
        }

        goal.setBalance(goal.getBalance().add(goal.getSweepAmount()));
        goal.setLastSweepAt(Instant.now());
        goal.setNextSweepAt(goal.getSweepFrequency().nextRunAfter(goal.getNextSweepAt()));
        vaultRepository.save(goal);
    }
}
