package com.manekpay.auth.exception;

public class ForbiddenInquiryAccessException extends RuntimeException {
    public ForbiddenInquiryAccessException() {
        super("You do not have access to this inquiry");
    }
}
