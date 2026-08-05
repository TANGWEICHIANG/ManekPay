package com.manekpay.ledger.exception;

public class DuplicateProxyException extends RuntimeException {
    public DuplicateProxyException() {
        super("This identifier is already linked to an account");
    }
}
