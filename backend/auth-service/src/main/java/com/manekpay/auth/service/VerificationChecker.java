package com.manekpay.auth.service;
import com.manekpay.auth.dto.VerificationResult;

public interface VerificationChecker {
    VerificationResult check(byte[] documentData, String declaredDataJson);
}
