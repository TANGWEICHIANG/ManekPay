package com.manekpay.auth.dto;
import com.manekpay.auth.entity.InquiryStatus;

import java.util.List;
import java.util.UUID;

public record InquiryResponse(UUID inquiryId, InquiryStatus status, List<VerificationSummary> verifications) {
}
