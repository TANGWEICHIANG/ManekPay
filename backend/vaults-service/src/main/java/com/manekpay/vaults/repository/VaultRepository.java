package com.manekpay.vaults.repository;

import com.manekpay.vaults.entity.Vault;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VaultRepository extends JpaRepository<Vault, UUID> {

    Optional<Vault> findByCustomerIdAndNameIsNull(UUID customerId);

    List<Vault> findByCustomerIdAndNameIsNotNull(UUID customerId);

    Optional<Vault> findByCustomerIdAndIdAndNameIsNotNull(UUID customerId, UUID id);

    List<Vault> findBySweepActiveTrueAndNextSweepAtLessThanEqual(Instant instant);
}
