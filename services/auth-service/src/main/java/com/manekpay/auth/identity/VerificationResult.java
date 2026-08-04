package com.manekpay.auth.identity;

public record VerificationResult(boolean passed, String resultDetailJson) {
}
