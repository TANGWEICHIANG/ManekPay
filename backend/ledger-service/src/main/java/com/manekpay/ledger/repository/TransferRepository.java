package com.manekpay.ledger.repository;

import com.manekpay.ledger.entity.Transfer;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Extends the bare Repository marker, not JpaRepository, so delete/deleteById/deleteAll never
// exist on this interface's compiled API - ldg04_transfers is append-only (see the V9 migration,
// which enforces the same rule at the database level as a second, independent layer). Every
// method below is one this codebase actually calls; nothing broader is exposed.
public interface TransferRepository extends Repository<Transfer, UUID> {

    Transfer save(Transfer transfer);

    Optional<Transfer> findById(UUID id);

    @Query("select t from Transfer t where t.fromWalletId in :walletIds or t.toWalletId in :walletIds order by t.createdAt desc")
    List<Transfer> findByWalletIdsOrderByCreatedAtDesc(@Param("walletIds") List<UUID> walletIds);
}
