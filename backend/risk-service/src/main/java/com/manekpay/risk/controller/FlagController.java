package com.manekpay.risk.controller;

import com.manekpay.risk.dto.FlagResponse;
import com.manekpay.risk.dto.FlagsResponse;
import com.manekpay.risk.repository.RiskFlagRepository;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class FlagController {

    private final RiskFlagRepository riskFlagRepository;

    public FlagController(RiskFlagRepository riskFlagRepository) {
        this.riskFlagRepository = riskFlagRepository;
    }

    @GetMapping("/flags/me")
    public FlagsResponse myFlags(@AuthenticationPrincipal Jwt jwt) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        var flags = riskFlagRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(f -> new FlagResponse(f.getId(), f.getTransactionId(), f.getRule(), f.getDetail(), f.getCreatedAt()))
                .toList();
        return new FlagsResponse(flags);
    }
}
