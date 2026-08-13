package com.manekpay.vaults.service;

import com.manekpay.vaults.dto.CreateGoalRequest;
import com.manekpay.vaults.dto.GoalResponse;
import com.manekpay.vaults.dto.UpdateGoalRequest;
import com.manekpay.vaults.entity.Vault;
import com.manekpay.vaults.exception.GoalNotFoundException;
import com.manekpay.vaults.repository.VaultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class GoalService {

    private final VaultRepository vaultRepository;

    public GoalService(VaultRepository vaultRepository) {
        this.vaultRepository = vaultRepository;
    }

    @Transactional
    public GoalResponse create(UUID customerId, CreateGoalRequest request) {
        Vault goal = new Vault(customerId, request.name(), request.currency(), request.targetAmount(),
                request.sweepAmount(), request.sweepFrequency());
        return toResponse(vaultRepository.save(goal));
    }

    public List<GoalResponse> list(UUID customerId) {
        return vaultRepository.findByCustomerIdAndNameIsNotNull(customerId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public GoalResponse update(UUID customerId, UUID goalId, UpdateGoalRequest request) {
        Vault goal = vaultRepository.findByCustomerIdAndIdAndNameIsNotNull(customerId, goalId)
                .orElseThrow(GoalNotFoundException::new);
        if (request.sweepAmount() != null) {
            goal.setSweepAmount(request.sweepAmount());
        }
        if (request.sweepFrequency() != null) {
            goal.setSweepFrequency(request.sweepFrequency());
        }
        if (request.sweepActive() != null) {
            boolean wasInactive = !goal.isSweepActive();
            goal.setSweepActive(request.sweepActive());
            // Resuming a paused goal restarts its schedule from now, rather than replaying every
            // period it missed while paused as a burst of back-to-back sweeps. Gated on
            // wasInactive so re-sending sweepActive:true on an already-active goal never shifts
            // nextSweepAt - that value is half of the scheduler's idempotency key, and shifting it
            // for a goal that was never paused would defeat retry-safety of an in-flight sweep.
            if (request.sweepActive() && wasInactive && goal.getNextSweepAt().isBefore(Instant.now())) {
                goal.setNextSweepAt(Instant.now());
            }
        }
        return toResponse(vaultRepository.save(goal));
    }

    private GoalResponse toResponse(Vault goal) {
        return new GoalResponse(goal.getId(), goal.getName(), goal.getCurrency(), goal.getBalance(),
                goal.getTargetAmount(), goal.getSweepAmount(), goal.getSweepFrequency(), goal.isSweepActive(),
                goal.getNextSweepAt(), goal.getLastSweepAt());
    }
}
