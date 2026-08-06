package com.manekpay.vaults.dto;

import com.manekpay.vaults.entity.Currency;

import java.math.BigDecimal;
import java.util.UUID;

public record VaultResponse(UUID vaultId, Currency currency, BigDecimal balance) {
}
