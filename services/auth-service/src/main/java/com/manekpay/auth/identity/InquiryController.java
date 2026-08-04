package com.manekpay.auth.identity;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/inquiries")
public class InquiryController {

    private final InquiryRepository inquiryRepository;
    private final VerificationRepository verificationRepository;

    public InquiryController(InquiryRepository inquiryRepository, VerificationRepository verificationRepository) {
        this.inquiryRepository = inquiryRepository;
        this.verificationRepository = verificationRepository;
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
        Inquiry inquiry = loadOwned(id);
        return toResponse(inquiry);
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
