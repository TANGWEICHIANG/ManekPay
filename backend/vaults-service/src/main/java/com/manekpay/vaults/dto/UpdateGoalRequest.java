package com.manekpay.vaults.dto;

import com.manekpay.vaults.entity.SweepFrequency;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

// All fields optional - PATCH only touches what's provided.
public record UpdateGoalRequest(@DecimalMin("0.01") BigDecimal sweepAmount, SweepFrequency sweepFrequency,
                                 Boolean sweepActive) {
}
