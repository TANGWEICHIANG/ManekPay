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

    // findByCustomerIdAndAssetIdForUpdate takes a row lock (SELECT ... FOR UPDATE, mirroring
    // ledger-service's WalletRepository.findByIdForUpdate) so two concurrent repeat purchases of
    // an asset the customer already holds can't both read the same shares total and silently
    // overwrite each other's update - the second transaction blocks until the first commits,
    // then reads the already-updated total. For a customer's FIRST purchase of an asset (no row
    // to lock yet), two concurrent buys can still both attempt to INSERT and race on the
    // unique(customer_id, asset_id) constraint - the loser's DataIntegrityViolationException
    // surfaces as a 409 via ApiExceptionHandler rather than being silently retried, same backstop
    // pattern as ledger-service's own idempotency-key race handling.
    @Transactional
    public Trade buy(UUID customerId, String assetSymbol, BigDecimal amount, String idempotencyKey) {
        Asset asset = assetRepository.findBySymbol(assetSymbol).orElseThrow(AssetNotFoundException::new);
        BigDecimal shares = amount.divide(asset.getPricePerShare(), 4, RoundingMode.HALF_EVEN);

        Trade trade = tradeRepository.save(new Trade(customerId, asset.getId(), amount, shares, asset.getPricePerShare(), idempotencyKey));

        Holding holding = holdingRepository.findByCustomerIdAndAssetIdForUpdate(customerId, asset.getId())
                .orElseGet(() -> new Holding(customerId, asset.getId()));
        holding.setShares(holding.getShares().add(shares));
        holdingRepository.save(holding);

        return trade;
    }
}
