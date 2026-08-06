package com.manekpay.wealth.repository;

import com.manekpay.wealth.entity.Holding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HoldingRepository extends JpaRepository<Holding, UUID> {
    Optional<Holding> findByCustomerIdAndAssetId(UUID customerId, UUID assetId);
    List<Holding> findByCustomerId(UUID customerId);
}
