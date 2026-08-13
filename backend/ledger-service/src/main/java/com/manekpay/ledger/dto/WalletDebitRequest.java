package com.manekpay.ledger.dto;

import com.manekpay.ledger.entity.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletDebitRequest(@NotNull UUID customerId, @NotNull Currency currency,
                                  @NotNull @DecimalMin("0.01") BigDecimal amount) {
}
