package com.manekpay.auth.dto;

import com.manekpay.auth.entity.KycStatus;

import java.util.UUID;

public record MeResponse(UUID customerId, String email, String fullName, KycStatus kycStatus) {
}
