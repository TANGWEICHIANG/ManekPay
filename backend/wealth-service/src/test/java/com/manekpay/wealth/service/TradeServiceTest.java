package com.manekpay.wealth.service;

import com.manekpay.wealth.entity.Asset;
import com.manekpay.wealth.entity.Holding;
import com.manekpay.wealth.entity.Trade;
import com.manekpay.wealth.exception.AssetNotFoundException;
import com.manekpay.wealth.repository.AssetRepository;
import com.manekpay.wealth.repository.HoldingRepository;
import com.manekpay.wealth.repository.TradeRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock
    private AssetRepository assetRepository;
    @Mock
    private TradeRepository tradeRepository;
    @Mock
    private HoldingRepository holdingRepository;

    @Test
    void computesFractionalSharesToFourDecimalPlacesUsingHalfEvenRounding() throws Exception {
        TradeService service = new TradeService(assetRepository, tradeRepository, holdingRepository);
        UUID customerId = UUID.randomUUID();
        Asset asset = newAsset("AAPL", "Apple Inc.", new BigDecimal("190.0000"), false);
        when(assetRepository.findBySymbol("AAPL")).thenReturn(Optional.of(asset));
        when(holdingRepository.findByCustomerIdAndAssetIdForUpdate(customerId, asset.getId())).thenReturn(Optional.empty());
        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Trade trade = service.buy(customerId, "AAPL", new BigDecimal("100.0000"), "key-1");

        assertThat(trade.getShares()).isEqualByComparingTo("0.5263");
    }

    @Test
    void throwsWhenTheAssetSymbolIsUnknown() {
        TradeService service = new TradeService(assetRepository, tradeRepository, holdingRepository);
        when(assetRepository.findBySymbol("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buy(UUID.randomUUID(), "NOPE", new BigDecimal("100.0000"), "key-1"))
                .isInstanceOf(AssetNotFoundException.class);
    }

    @Test
    void createsANewHoldingOnACustomersFirstPurchaseOfAnAsset() throws Exception {
        TradeService service = new TradeService(assetRepository, tradeRepository, holdingRepository);
        UUID customerId = UUID.randomUUID();
        Asset asset = newAsset("AAPL", "Apple Inc.", new BigDecimal("190.0000"), false);
        when(assetRepository.findBySymbol("AAPL")).thenReturn(Optional.of(asset));
        when(holdingRepository.findByCustomerIdAndAssetIdForUpdate(customerId, asset.getId())).thenReturn(Optional.empty());
        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.buy(customerId, "AAPL", new BigDecimal("190.0000"), "key-1");

        ArgumentCaptor<Holding> holdingCaptor = ArgumentCaptor.forClass(Holding.class);
        verify(holdingRepository).save(holdingCaptor.capture());
        assertThat(holdingCaptor.getValue().getCustomerId()).isEqualTo(customerId);
        assertThat(holdingCaptor.getValue().getShares()).isEqualByComparingTo("1.0000");
    }

    @Test
    void addsToAnExistingHoldingOnARepeatPurchase() throws Exception {
        TradeService service = new TradeService(assetRepository, tradeRepository, holdingRepository);
        UUID customerId = UUID.randomUUID();
        Asset asset = newAsset("AAPL", "Apple Inc.", new BigDecimal("190.0000"), false);
        Holding existing = new Holding(customerId, asset.getId());
        existing.setShares(new BigDecimal("2.0000"));
        when(assetRepository.findBySymbol("AAPL")).thenReturn(Optional.of(asset));
        when(holdingRepository.findByCustomerIdAndAssetIdForUpdate(customerId, asset.getId())).thenReturn(Optional.of(existing));
        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.buy(customerId, "AAPL", new BigDecimal("190.0000"), "key-1");

        assertThat(existing.getShares()).isEqualByComparingTo("3.0000");
        // The row-locking read is what prevents two concurrent repeat purchases from both reading
        // the same shares total and silently overwriting each other's update - pin the actual
        // repository method being called, not just the observable outcome.
        verify(holdingRepository).findByCustomerIdAndAssetIdForUpdate(customerId, asset.getId());
    }

    // Asset's constructor is package-private-by-omission (protected, JPA-only) with no public
    // constructor per Task 1's design (rows only ever come from the seed migration) - tests build
    // one via reflection rather than adding an application-only constructor just for test setup.
    private Asset newAsset(String symbol, String name, BigDecimal pricePerShare, boolean shariahCompliant) throws Exception {
        Constructor<Asset> constructor = Asset.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Asset asset = constructor.newInstance();
        setField(asset, "id", UUID.randomUUID());
        setField(asset, "symbol", symbol);
        setField(asset, "name", name);
        setField(asset, "pricePerShare", pricePerShare);
        setField(asset, "shariahCompliant", shariahCompliant);
        return asset;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
