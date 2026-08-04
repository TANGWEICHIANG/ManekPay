package com.manekpay.auth.dto;

public record VerificationResult(boolean passed, String resultDetailJson) {
}
