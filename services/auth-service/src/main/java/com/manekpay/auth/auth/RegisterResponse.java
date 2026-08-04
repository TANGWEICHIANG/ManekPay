package com.manekpay.auth.auth;

import com.manekpay.auth.customer.KycStatus;

import java.util.UUID;

public record RegisterResponse(UUID customerId, KycStatus kycStatus) {
}
