package com.manekpay.ledger.repository;

import com.manekpay.ledger.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByCustomerId(UUID customerId);
    Optional<Account> findByAccountNumber(String accountNumber);
}
