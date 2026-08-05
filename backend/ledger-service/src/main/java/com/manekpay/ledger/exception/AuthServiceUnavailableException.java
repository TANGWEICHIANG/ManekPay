package com.manekpay.ledger.exception;

public class AuthServiceUnavailableException extends RuntimeException {
    public AuthServiceUnavailableException(Throwable cause) {
        super("Could not verify KYC status - please try again shortly", cause);
    }
}
