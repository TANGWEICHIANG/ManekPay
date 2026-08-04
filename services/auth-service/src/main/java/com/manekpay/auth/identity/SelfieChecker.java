package com.manekpay.auth.identity;

import org.springframework.stereotype.Component;

@Component
public class SelfieChecker implements VerificationChecker {

    @Override
    public VerificationResult check(byte[] documentData, String declaredDataJson) {
        boolean passed = documentData != null && documentData.length > 0;
        String detail = passed
                ? "{\"note\":\"simulated selfie check - image present\"}"
                : "{\"note\":\"simulated selfie check - no image provided\"}";
        return new VerificationResult(passed, detail);
    }
}
