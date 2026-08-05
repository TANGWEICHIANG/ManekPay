package com.manekpay.auth.service;
import com.manekpay.auth.dto.VerificationResult;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GovernmentIdCheckerTest {

    private final GovernmentIdChecker checker = new GovernmentIdChecker();

    @Test
    void validNricAndMatchingDobPasses() {
        String declared = "{\"nric\":\"900101-14-5678\",\"dob\":\"1990-01-01\",\"nationality\":\"Malaysian\"}";
        VerificationResult result = checker.check(new byte[]{1}, declared);
        assertThat(result.passed()).isTrue();
    }

    @Test
    void nricNotTwelveDigitsFails() {
        String declared = "{\"nric\":\"900101-14-567\",\"dob\":\"1990-01-01\",\"nationality\":\"Malaysian\"}";
        VerificationResult result = checker.check(new byte[]{1}, declared);
        assertThat(result.passed()).isFalse();
        assertThat(result.resultDetailJson()).contains("nricFormatValid\":false");
    }

    @Test
    void dobMismatchWithNricFails() {
        String declared = "{\"nric\":\"900101-14-5678\",\"dob\":\"1991-05-05\",\"nationality\":\"Malaysian\"}";
        VerificationResult result = checker.check(new byte[]{1}, declared);
        assertThat(result.passed()).isFalse();
        assertThat(result.resultDetailJson()).contains("dobMatch\":false");
    }

    @Test
    void invalidBirthplaceCodeFails() {
        String declared = "{\"nric\":\"900101-99-5678\",\"dob\":\"1990-01-01\",\"nationality\":\"Malaysian\"}";
        VerificationResult result = checker.check(new byte[]{1}, declared);
        assertThat(result.passed()).isFalse();
        assertThat(result.resultDetailJson()).contains("birthplaceCodeValid\":false");
    }
}
