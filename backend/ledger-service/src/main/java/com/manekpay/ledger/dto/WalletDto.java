package com.manekpay.ledger.dto;

import com.manekpay.ledger.entity.Currency;

import java.math.BigDecimal;

public record WalletDto(Currency currency, BigDecimal balance) {
}
