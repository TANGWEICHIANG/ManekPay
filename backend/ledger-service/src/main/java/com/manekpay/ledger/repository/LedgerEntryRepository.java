package com.manekpay.ledger.repository;

import com.manekpay.ledger.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    List<LedgerEntry> findByWalletId(UUID walletId);

    List<LedgerEntry> findByTransferId(UUID transferId);
}
