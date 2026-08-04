package com.manekpay.auth.identity;

public interface VerificationChecker {
    VerificationResult check(byte[] documentData, String declaredDataJson);
}
