package com.manekpay.auth.identity;

import java.util.UUID;

public record VerificationSummary(UUID verificationId, VerificationType type, VerificationStatus status, String resultDetail) {
}
