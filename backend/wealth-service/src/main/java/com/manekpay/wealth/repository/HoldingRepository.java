package com.manekpay.wealth.repository;

import com.manekpay.wealth.entity.Holding;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HoldingRepository extends JpaRepository<Holding, UUID> {
    List<Holding> findByCustomerId(UUID customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Holding h where h.customerId = :customerId and h.assetId = :assetId")
    Optional<Holding> findByCustomerIdAndAssetIdForUpdate(@Param("customerId") UUID customerId, @Param("assetId") UUID assetId);
}
