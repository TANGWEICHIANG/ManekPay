package com.manekpay.wealth.repository;

import com.manekpay.wealth.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<Asset, UUID> {
    Optional<Asset> findBySymbol(String symbol);
    List<Asset> findByShariahCompliant(boolean shariahCompliant);
}
