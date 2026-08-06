package com.manekpay.wealth.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateTradeRequest(@NotBlank String assetSymbol, @NotNull @DecimalMin(value = "0.01") BigDecimal amount) {
}
