package com.manekpay.vaults.repository;

import com.manekpay.vaults.entity.Vault;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VaultRepository extends JpaRepository<Vault, UUID> {
    Optional<Vault> findByCustomerId(UUID customerId);
}
