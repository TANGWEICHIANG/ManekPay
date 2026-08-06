package com.manekpay.wealth.dto;

import java.math.BigDecimal;

public record HoldingResponse(String assetSymbol, String assetName, BigDecimal shares, boolean shariahCompliant) {
}
