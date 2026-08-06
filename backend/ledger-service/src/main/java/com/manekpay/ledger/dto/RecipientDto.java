package com.manekpay.ledger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecipientDto(@NotNull RecipientType type, @NotBlank String value) {
}
