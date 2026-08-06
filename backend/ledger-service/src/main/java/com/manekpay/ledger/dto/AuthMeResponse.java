package com.manekpay.ledger.dto;

import java.util.UUID;

public record AuthMeResponse(UUID customerId, String email, String fullName, String kycStatus) {
}
