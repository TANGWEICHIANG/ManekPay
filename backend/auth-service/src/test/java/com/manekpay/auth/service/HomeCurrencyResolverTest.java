package com.manekpay.auth.service;

import com.manekpay.auth.entity.Verification;
import com.manekpay.auth.entity.VerificationStatus;
import com.manekpay.auth.entity.VerificationType;
import com.manekpay.auth.repository.VerificationRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeCurrencyResolverTest {

    @Mock
    private VerificationRepository verificationRepository;

    @Test
    void resolvesCurrencyFromMostRecentPassedGovernmentIdVerification() {
        UUID customerId = UUID.randomUUID();
        Verification verification = new Verification(UUID.randomUUID(), VerificationType.GOVERNMENT_ID, null,
                "{\"nric\":\"900101-14-5678\",\"dob\":\"1990-01-01\",\"nationality\":\"Singaporean\"}");
        when(verificationRepository.findByCustomerIdAndTypeAndStatusOrderByCreatedAtDesc(
                customerId, VerificationType.GOVERNMENT_ID, VerificationStatus.PASSED))
                .thenReturn(List.of(verification));

        HomeCurrencyResolver resolver = new HomeCurrencyResolver(verificationRepository);

        assertThat(resolver.resolve(customerId)).isEqualTo("SGD");
    }

    @Test
    void defaultsToMyrWhenNoPassedVerificationExists() {
        UUID customerId = UUID.randomUUID();
        when(verificationRepository.findByCustomerIdAndTypeAndStatusOrderByCreatedAtDesc(
                customerId, VerificationType.GOVERNMENT_ID, VerificationStatus.PASSED))
                .thenReturn(List.of());

        HomeCurrencyResolver resolver = new HomeCurrencyResolver(verificationRepository);

        assertThat(resolver.resolve(customerId)).isEqualTo("MYR");
    }

    @Test
    void defaultsToMyrWhenDeclaredDataIsMalformed() {
        UUID customerId = UUID.randomUUID();
        Verification verification = new Verification(UUID.randomUUID(), VerificationType.GOVERNMENT_ID, null, "not-json");
        when(verificationRepository.findByCustomerIdAndTypeAndStatusOrderByCreatedAtDesc(
                customerId, VerificationType.GOVERNMENT_ID, VerificationStatus.PASSED))
                .thenReturn(List.of(verification));

        HomeCurrencyResolver resolver = new HomeCurrencyResolver(verificationRepository);

        assertThat(resolver.resolve(customerId)).isEqualTo("MYR");
    }

    @Test
    void defaultsToMyrWhenNationalityHasNoKnownMapping() {
        UUID customerId = UUID.randomUUID();
        Verification verification = new Verification(UUID.randomUUID(), VerificationType.GOVERNMENT_ID, null,
                "{\"nric\":\"900101-14-5678\",\"dob\":\"1990-01-01\",\"nationality\":\"Martian\"}");
        when(verificationRepository.findByCustomerIdAndTypeAndStatusOrderByCreatedAtDesc(
                customerId, VerificationType.GOVERNMENT_ID, VerificationStatus.PASSED))
                .thenReturn(List.of(verification));

        HomeCurrencyResolver resolver = new HomeCurrencyResolver(verificationRepository);

        assertThat(resolver.resolve(customerId)).isEqualTo("MYR");
    }
}
