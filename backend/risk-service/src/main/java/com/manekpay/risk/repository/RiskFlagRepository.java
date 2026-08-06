package com.manekpay.risk.repository;

import com.manekpay.risk.entity.RiskFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RiskFlagRepository extends JpaRepository<RiskFlag, UUID> {
    List<RiskFlag> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
