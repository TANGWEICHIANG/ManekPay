package com.manekpay.wealth.controller;

import com.manekpay.wealth.dto.HoldingResponse;
import com.manekpay.wealth.dto.HoldingsResponse;
import com.manekpay.wealth.entity.Asset;
import com.manekpay.wealth.repository.AssetRepository;
import com.manekpay.wealth.repository.HoldingRepository;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class HoldingController {

    private final HoldingRepository holdingRepository;
    private final AssetRepository assetRepository;

    public HoldingController(HoldingRepository holdingRepository, AssetRepository assetRepository) {
        this.holdingRepository = holdingRepository;
        this.assetRepository = assetRepository;
    }

    @GetMapping("/holdings/me")
    public HoldingsResponse myHoldings(@AuthenticationPrincipal Jwt jwt) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        Map<UUID, Asset> assetsById = assetRepository.findAll().stream()
                .collect(Collectors.toMap(Asset::getId, a -> a));
        List<HoldingResponse> holdings = holdingRepository.findByCustomerId(customerId).stream()
                .map(h -> {
                    Asset asset = assetsById.get(h.getAssetId());
                    return new HoldingResponse(asset.getSymbol(), asset.getName(), h.getShares(), asset.isShariahCompliant());
                })
                .toList();
        return new HoldingsResponse(holdings);
    }
}
