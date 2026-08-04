package com.manekpay.auth.identity;

import java.util.List;
import java.util.UUID;

public record InquiryResponse(UUID inquiryId, InquiryStatus status, List<VerificationSummary> verifications) {
}
