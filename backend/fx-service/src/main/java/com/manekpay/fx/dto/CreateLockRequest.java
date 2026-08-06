package com.manekpay.fx.dto;

import com.manekpay.fx.entity.Currency;
import jakarta.validation.constraints.NotNull;

public record CreateLockRequest(@NotNull Currency from, @NotNull Currency to) {
}
