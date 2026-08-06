package com.manekpay.wealth.controller;

import com.manekpay.wealth.dto.AssetResponse;
import com.manekpay.wealth.dto.AssetsResponse;
import com.manekpay.wealth.entity.Asset;
import com.manekpay.wealth.repository.AssetRepository;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AssetController {

    private final AssetRepository assetRepository;

    public AssetController(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    @GetMapping("/assets")
    public AssetsResponse listAssets(@RequestParam(required = false) Boolean shariahCompliant) {
        List<Asset> assets = shariahCompliant != null
                ? assetRepository.findByShariahCompliant(shariahCompliant)
                : assetRepository.findAll();
        List<AssetResponse> response = assets.stream()
                .map(a -> new AssetResponse(a.getId(), a.getSymbol(), a.getName(), a.getPricePerShare(), a.isShariahCompliant()))
                .toList();
        return new AssetsResponse(response);
    }
}
