package com.manekpay.ledger.repository;

import com.manekpay.ledger.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    @Query("select t from Transfer t where t.fromWalletId in :walletIds or t.toWalletId in :walletIds order by t.createdAt desc")
    List<Transfer> findByWalletIdsOrderByCreatedAtDesc(@Param("walletIds") List<UUID> walletIds);
}
