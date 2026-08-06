package com.manekpay.wealth.service;

import com.manekpay.wealth.entity.Asset;
import com.manekpay.wealth.entity.Holding;
import com.manekpay.wealth.entity.Trade;
import com.manekpay.wealth.exception.AssetNotFoundException;
import com.manekpay.wealth.repository.AssetRepository;
import com.manekpay.wealth.repository.HoldingRepository;
import com.manekpay.wealth.repository.TradeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class TradeService {

    private final AssetRepository assetRepository;
    private final TradeRepository tradeRepository;
    private final HoldingRepository holdingRepository;

    public TradeService(AssetRepository assetRepository, TradeRepository tradeRepository, HoldingRepository holdingRepository) {
        this.assetRepository = assetRepository;
        this.tradeRepository = tradeRepository;
        this.holdingRepository = holdingRepository;
    }

    // Two concurrent buys of the SAME asset by the SAME customer (different idempotency keys,
    // so genuinely two separate intended purchases) could both read "no holding yet" and race on
    // the unique(customer_id, asset_id) constraint - the loser's DataIntegrityViolationException
    // surfaces as a 409 via ApiExceptionHandler rather than being silently retried, same backstop
    // pattern as ledger-service's own idempotency-key race handling. This is a narrow enough
    // window (two orders for the identical ticker within milliseconds) that a "please retry"
    // response is an acceptable simplification for this phase.
    @Transactional
    public Trade buy(UUID customerId, String assetSymbol, BigDecimal amount, String idempotencyKey) {
        Asset asset = assetRepository.findBySymbol(assetSymbol).orElseThrow(AssetNotFoundException::new);
        BigDecimal shares = amount.divide(asset.getPricePerShare(), 4, RoundingMode.HALF_EVEN);

        Trade trade = tradeRepository.save(new Trade(customerId, asset.getId(), amount, shares, asset.getPricePerShare(), idempotencyKey));

        Holding holding = holdingRepository.findByCustomerIdAndAssetId(customerId, asset.getId())
                .orElseGet(() -> new Holding(customerId, asset.getId()));
        holding.setShares(holding.getShares().add(shares));
        holdingRepository.save(holding);

        return trade;
    }
}
