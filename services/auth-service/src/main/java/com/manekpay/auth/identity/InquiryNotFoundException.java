package com.manekpay.auth.identity;

public class InquiryNotFoundException extends RuntimeException {
    public InquiryNotFoundException() {
        super("Inquiry not found");
    }
}
