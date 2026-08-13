package com.manekpay.vaults.dto;

import com.manekpay.vaults.entity.Currency;
import com.manekpay.vaults.entity.SweepFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateGoalRequest(@NotBlank @Size(max = 100) String name, @NotNull Currency currency,
                                 @NotNull @DecimalMin("0.01") BigDecimal targetAmount,
                                 @NotNull @DecimalMin("0.01") BigDecimal sweepAmount,
                                 @NotNull SweepFrequency sweepFrequency) {
}
