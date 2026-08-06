package com.manekpay.ledger.dto;

import com.manekpay.ledger.entity.ProxyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProxyRequest(@NotNull ProxyType type, @NotBlank String value) {
}
