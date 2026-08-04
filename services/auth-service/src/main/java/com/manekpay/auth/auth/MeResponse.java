package com.manekpay.auth.auth;

import com.manekpay.auth.customer.KycStatus;

import java.util.UUID;

public record MeResponse(UUID customerId, String email, String fullName, KycStatus kycStatus) {
}
