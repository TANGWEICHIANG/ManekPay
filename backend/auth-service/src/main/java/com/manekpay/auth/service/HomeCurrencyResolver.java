package com.manekpay.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manekpay.auth.dto.GovernmentIdDeclaration;
import com.manekpay.auth.entity.Verification;
import com.manekpay.auth.entity.VerificationStatus;
import com.manekpay.auth.entity.VerificationType;
import com.manekpay.auth.repository.VerificationRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class HomeCurrencyResolver {

    private static final String DEFAULT_CURRENCY = "MYR";

    // Intentionally small, illustrative mapping given `nationality` is unvalidated free text
    // from GovernmentIdDeclaration - not an exhaustive ISO country/currency table. Extending it
    // is a follow-up concern, not blocking this phase.
    private static final Map<String, String> NATIONALITY_TO_CURRENCY = Map.ofEntries(
            Map.entry("malaysian", "MYR"),
            Map.entry("singaporean", "SGD"),
            Map.entry("american", "USD"),
            Map.entry("british", "GBP"),
            Map.entry("german", "EUR"),
            Map.entry("french", "EUR"),
            Map.entry("italian", "EUR"),
            Map.entry("spanish", "EUR"),
            Map.entry("dutch", "EUR")
    );

    private final VerificationRepository verificationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HomeCurrencyResolver(VerificationRepository verificationRepository) {
        this.verificationRepository = verificationRepository;
    }

    public String resolve(UUID customerId) {
        List<Verification> verifications = verificationRepository
                .findByCustomerIdAndTypeAndStatusOrderByCreatedAtDesc(
                        customerId, VerificationType.GOVERNMENT_ID, VerificationStatus.PASSED);
        if (verifications.isEmpty()) {
            return DEFAULT_CURRENCY;
        }
        return currencyFromDeclaredData(verifications.get(0));
    }

    private String currencyFromDeclaredData(Verification verification) {
        try {
            GovernmentIdDeclaration declaration =
                    objectMapper.readValue(verification.getDeclaredData(), GovernmentIdDeclaration.class);
            if (declaration.nationality() == null) {
                return DEFAULT_CURRENCY;
            }
            return NATIONALITY_TO_CURRENCY.getOrDefault(
                    declaration.nationality().trim().toLowerCase(), DEFAULT_CURRENCY);
        } catch (Exception e) {
            return DEFAULT_CURRENCY;
        }
    }
}
