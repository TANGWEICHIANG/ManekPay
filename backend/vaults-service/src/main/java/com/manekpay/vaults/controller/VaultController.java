package com.manekpay.vaults.controller;

import com.manekpay.vaults.dto.VaultResponse;
import com.manekpay.vaults.entity.Vault;
import com.manekpay.vaults.exception.VaultNotFoundException;
import com.manekpay.vaults.repository.VaultRepository;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class VaultController {

    private final VaultRepository vaultRepository;

    public VaultController(VaultRepository vaultRepository) {
        this.vaultRepository = vaultRepository;
    }

    @GetMapping("/me")
    public VaultResponse me(@AuthenticationPrincipal Jwt jwt) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        Vault vault = vaultRepository.findByCustomerIdAndNameIsNull(customerId).orElseThrow(VaultNotFoundException::new);
        return new VaultResponse(vault.getId(), vault.getCurrency(), vault.getBalance());
    }
}
