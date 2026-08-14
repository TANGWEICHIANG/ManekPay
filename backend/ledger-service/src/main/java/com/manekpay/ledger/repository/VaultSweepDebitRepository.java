package com.manekpay.ledger.repository;

import com.manekpay.ledger.entity.VaultSweepDebit;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

// Extends the bare Repository marker, not JpaRepository, so delete/deleteById/deleteAll never
// exist on this interface's compiled API - ldg06_vault_sweep_debits is append-only (see the V9
// migration, which enforces the same rule at the database level as a second, independent layer).
// Every method below is one this codebase actually calls; nothing broader is exposed.
public interface VaultSweepDebitRepository extends Repository<VaultSweepDebit, UUID> {

    VaultSweepDebit save(VaultSweepDebit debit);

    Optional<VaultSweepDebit> findByReference(String reference);
}
