package com.manekpay.ledger.repository;

import com.manekpay.ledger.entity.LedgerEntry;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

// Extends the bare Repository marker, not JpaRepository, so delete/deleteById/deleteAll never
// exist on this interface's compiled API - ldg05_ledger_entries is append-only (see the V9
// migration, which enforces the same rule at the database level as a second, independent layer).
// Every method below is one this codebase actually calls; nothing broader is exposed.
public interface LedgerEntryRepository extends Repository<LedgerEntry, UUID> {

    LedgerEntry save(LedgerEntry entry);

    List<LedgerEntry> findByWalletId(UUID walletId);

    List<LedgerEntry> findByTransferId(UUID transferId);
}
