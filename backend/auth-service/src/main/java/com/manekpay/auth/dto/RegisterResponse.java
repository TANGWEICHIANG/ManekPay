package com.manekpay.auth.dto;

import com.manekpay.auth.entity.KycStatus;

import java.util.UUID;

public record RegisterResponse(UUID customerId, KycStatus kycStatus) {
}
