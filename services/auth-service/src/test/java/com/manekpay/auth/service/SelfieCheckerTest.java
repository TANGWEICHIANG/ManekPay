package com.manekpay.auth.service;
import com.manekpay.auth.dto.VerificationResult;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SelfieCheckerTest {

    private final SelfieChecker checker = new SelfieChecker();

    @Test
    void nonEmptyImagePasses() {
        VerificationResult result = checker.check(new byte[]{1, 2, 3}, "{}");
        assertThat(result.passed()).isTrue();
    }

    @Test
    void emptyImageFails() {
        VerificationResult result = checker.check(new byte[]{}, "{}");
        assertThat(result.passed()).isFalse();
    }

    @Test
    void nullImageFails() {
        VerificationResult result = checker.check(null, "{}");
        assertThat(result.passed()).isFalse();
    }
}
