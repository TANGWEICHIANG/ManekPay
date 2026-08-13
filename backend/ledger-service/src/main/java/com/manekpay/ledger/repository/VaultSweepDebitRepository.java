package com.manekpay.ledger.repository;

import com.manekpay.ledger.entity.VaultSweepDebit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VaultSweepDebitRepository extends JpaRepository<VaultSweepDebit, UUID> {

    Optional<VaultSweepDebit> findByReference(String reference);
}
