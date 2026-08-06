package com.manekpay.ledger.repository;

import com.manekpay.ledger.entity.Currency;
import com.manekpay.ledger.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    List<Wallet> findByAccountId(UUID accountId);

    Optional<Wallet> findByAccountIdAndCurrency(UUID accountId, Currency currency);

    Optional<Wallet> findByAccountIdIsNullAndCurrency(Currency currency);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.id = :id")
    Optional<Wallet> findByIdForUpdate(@Param("id") UUID id);
}
