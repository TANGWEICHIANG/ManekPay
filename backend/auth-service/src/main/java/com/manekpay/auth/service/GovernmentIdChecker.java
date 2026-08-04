package com.manekpay.auth.service;
import com.manekpay.auth.dto.VerificationResult;
import com.manekpay.auth.dto.GovernmentIdDeclaration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GovernmentIdChecker implements VerificationChecker {

    private static final Pattern NRIC_PATTERN = Pattern.compile("^(\\d{2})(\\d{2})(\\d{2})-(\\d{2})-\\d{4}$");
    private static final Set<String> VALID_BIRTHPLACE_CODES = Set.of(
            "01", "02", "03", "04", "05", "06", "07", "08", "09", "10",
            "11", "12", "13", "14", "15", "16", "21", "22", "23", "24",
            "25", "26", "27", "28", "29", "30", "31", "32", "33", "34",
            "35", "36", "37", "38", "39", "40", "41", "42", "43", "44",
            "45", "46", "47", "48", "49", "82"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public VerificationResult check(byte[] documentData, String declaredDataJson) {
        try {
            GovernmentIdDeclaration declaration = objectMapper.readValue(declaredDataJson, GovernmentIdDeclaration.class);
            Matcher matcher = NRIC_PATTERN.matcher(declaration.nric());
            boolean formatValid = matcher.matches();

            boolean birthplaceValid = false;
            boolean dobMatch = false;

            if (formatValid) {
                String yy = matcher.group(1);
                String mm = matcher.group(2);
                String dd = matcher.group(3);
                String birthplaceCode = matcher.group(4);

                birthplaceValid = VALID_BIRTHPLACE_CODES.contains(birthplaceCode);
                dobMatch = dobMatchesNric(yy, mm, dd, declaration.dob());
            }

            boolean passed = formatValid && birthplaceValid && dobMatch;
            String detail = String.format(
                    "{\"nricFormatValid\":%b,\"birthplaceCodeValid\":%b,\"dobMatch\":%b}",
                    formatValid, birthplaceValid, dobMatch);
            return new VerificationResult(passed, detail);
        } catch (Exception e) {
            return new VerificationResult(false, "{\"error\":\"could not parse declared data\"}");
        }
    }

    private boolean dobMatchesNric(String yy, String mm, String dd, String declaredDob) {
        try {
            LocalDate declared = LocalDate.parse(declaredDob, DateTimeFormatter.ISO_LOCAL_DATE);
            String declaredYy = String.format("%02d", declared.getYear() % 100);
            String declaredMm = String.format("%02d", declared.getMonthValue());
            String declaredDd = String.format("%02d", declared.getDayOfMonth());
            return yy.equals(declaredYy) && mm.equals(declaredMm) && dd.equals(declaredDd);
        } catch (Exception e) {
            return false;
        }
    }
}
