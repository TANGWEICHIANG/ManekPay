package com.manekpay.ledger.repository;

import com.manekpay.ledger.entity.AccountProxy;
import com.manekpay.ledger.entity.ProxyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountProxyRepository extends JpaRepository<AccountProxy, UUID> {
    Optional<AccountProxy> findByTypeAndValue(ProxyType type, String value);
    List<AccountProxy> findByAccountId(UUID accountId);
    boolean existsByTypeAndValue(ProxyType type, String value);
}
