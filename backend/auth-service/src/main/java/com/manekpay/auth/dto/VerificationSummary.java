package com.manekpay.auth.dto;
import com.manekpay.auth.entity.VerificationStatus;
import com.manekpay.auth.entity.VerificationType;

import java.util.UUID;

public record VerificationSummary(UUID verificationId, VerificationType type, VerificationStatus status, String resultDetail) {
}
