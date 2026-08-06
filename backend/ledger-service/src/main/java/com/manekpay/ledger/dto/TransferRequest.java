package com.manekpay.ledger.dto;

import com.manekpay.ledger.entity.Currency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferRequest(@Valid @NotNull RecipientDto recipient, @NotNull Currency sourceCurrency,
                               @NotNull Currency destCurrency, @NotNull @DecimalMin("0.01") BigDecimal amount) {
}
