package com.manekpay.wealth.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AssetResponse(UUID assetId, String symbol, String name, BigDecimal pricePerShare, boolean shariahCompliant) {
}
