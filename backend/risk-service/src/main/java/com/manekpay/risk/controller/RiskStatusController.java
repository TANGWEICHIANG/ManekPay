package com.manekpay.risk.controller;

import com.manekpay.risk.dto.RiskStatusResponse;
import com.manekpay.risk.entity.RiskFlag;
import com.manekpay.risk.repository.RiskFlagRepository;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@RestController
public class RiskStatusController {

    private static final int RESTRICTION_WINDOW_HOURS = 24;

    private final RiskFlagRepository riskFlagRepository;

    public RiskStatusController(RiskFlagRepository riskFlagRepository) {
        this.riskFlagRepository = riskFlagRepository;
    }

    @GetMapping("/internal/risk-status/{customerId}")
    public RiskStatusResponse status(@PathVariable UUID customerId) {
        List<RiskFlag> flags = riskFlagRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        if (flags.isEmpty()) {
            return new RiskStatusResponse(false, null);
        }
        Instant restrictedUntil = flags.get(0).getCreatedAt().plus(RESTRICTION_WINDOW_HOURS, ChronoUnit.HOURS);
        boolean restricted = !restrictedUntil.isBefore(Instant.now());
        return new RiskStatusResponse(restricted, restricted ? restrictedUntil : null);
    }
}
