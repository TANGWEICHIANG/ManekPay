package com.manekpay.auth.exception;
import com.manekpay.auth.entity.Inquiry;

public class InquiryNotFoundException extends RuntimeException {
    public InquiryNotFoundException() {
        super("Inquiry not found");
    }
}
