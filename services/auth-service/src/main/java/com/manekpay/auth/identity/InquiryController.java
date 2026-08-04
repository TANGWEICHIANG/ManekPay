package com.manekpay.auth.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manekpay.auth.customer.Customer;
import com.manekpay.auth.customer.CustomerRepository;
import com.manekpay.auth.customer.KycStatus;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/inquiries")
public class InquiryController {

    private final InquiryRepository inquiryRepository;
    private final VerificationRepository verificationRepository;
    private final CustomerRepository customerRepository;
    private final GovernmentIdChecker governmentIdChecker;
    private final SelfieChecker selfieChecker;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InquiryController(InquiryRepository inquiryRepository, VerificationRepository verificationRepository,
                              CustomerRepository customerRepository, GovernmentIdChecker governmentIdChecker,
                              SelfieChecker selfieChecker) {
        this.inquiryRepository = inquiryRepository;
        this.verificationRepository = verificationRepository;
        this.customerRepository = customerRepository;
        this.governmentIdChecker = governmentIdChecker;
        this.selfieChecker = selfieChecker;
    }

    static UUID currentCustomerId() {
        return UUID.fromString((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InquiryResponse create() {
        Inquiry inquiry = inquiryRepository.save(new Inquiry(currentCustomerId()));
        return toResponse(inquiry);
    }

    @GetMapping("/{id}")
    public InquiryResponse get(@PathVariable UUID id) {
        return toResponse(loadOwned(id));
    }

    @PostMapping(value = "/{id}/verifications/government-id", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public VerificationSummary submitGovernmentId(@PathVariable UUID id,
                                                   @RequestParam MultipartFile image,
                                                   @RequestParam String nric,
                                                   @RequestParam String dob,
                                                   @RequestParam String nationality) throws IOException {
        Inquiry inquiry = loadOwned(id);
        String declaredJson = objectMapper.writeValueAsString(new GovernmentIdDeclaration(nric, dob, nationality));
        Verification verification = new Verification(inquiry.getId(), VerificationType.GOVERNMENT_ID, image.getBytes(), declaredJson);
        VerificationResult result = governmentIdChecker.check(image.getBytes(), declaredJson);
        return submitVerification(inquiry, verification, result);
    }

    @PostMapping(value = "/{id}/verifications/selfie", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public VerificationSummary submitSelfie(@PathVariable UUID id, @RequestParam MultipartFile image) throws IOException {
        Inquiry inquiry = loadOwned(id);
        Verification verification = new Verification(inquiry.getId(), VerificationType.SELFIE, image.getBytes(), "{}");
        VerificationResult result = selfieChecker.check(image.getBytes(), "{}");
        return submitVerification(inquiry, verification, result);
    }

    @Transactional
    protected VerificationSummary submitVerification(Inquiry inquiry, Verification verification, VerificationResult result) {
        verification.setStatus(result.passed() ? VerificationStatus.PASSED : VerificationStatus.FAILED);
        verification.setResultDetail(result.resultDetailJson());
        Verification saved = verificationRepository.save(verification);

        List<Verification> all = verificationRepository.findByInquiryId(inquiry.getId());
        boolean hasPassedGovernmentId = all.stream().anyMatch(v -> v.getType() == VerificationType.GOVERNMENT_ID && v.getStatus() == VerificationStatus.PASSED);
        boolean hasPassedSelfie = all.stream().anyMatch(v -> v.getType() == VerificationType.SELFIE && v.getStatus() == VerificationStatus.PASSED);

        // NOTE: this updates the database only — the customer's currently-held access token
        // (if any) still carries whatever kycStatus was true at login time, for up to its
        // remaining lifetime. See JwtService.issueAccessToken's note.
        if (hasPassedGovernmentId && hasPassedSelfie) {
            inquiry.setStatus(InquiryStatus.APPROVED);
            inquiryRepository.save(inquiry);
            Customer customer = customerRepository.findById(inquiry.getCustomerId()).orElseThrow();
            customer.setKycStatus(KycStatus.APPROVED);
            customerRepository.save(customer);
        } else if (inquiry.getStatus() == InquiryStatus.CREATED) {
            inquiry.setStatus(InquiryStatus.IN_PROGRESS);
            inquiryRepository.save(inquiry);
        }

        return new VerificationSummary(saved.getId(), saved.getType(), saved.getStatus(), saved.getResultDetail());
    }

    Inquiry loadOwned(UUID id) {
        Inquiry inquiry = inquiryRepository.findById(id).orElseThrow(InquiryNotFoundException::new);
        if (!inquiry.getCustomerId().equals(currentCustomerId())) {
            throw new ForbiddenInquiryAccessException();
        }
        return inquiry;
    }

    InquiryResponse toResponse(Inquiry inquiry) {
        List<VerificationSummary> verifications = verificationRepository.findByInquiryId(inquiry.getId()).stream()
                .map(v -> new VerificationSummary(v.getId(), v.getType(), v.getStatus(), v.getResultDetail()))
                .collect(Collectors.toList());
        return new InquiryResponse(inquiry.getId(), inquiry.getStatus(), verifications);
    }
}
