package com.manekpay.ledger.exception;

public class KycNotApprovedException extends RuntimeException {
    public KycNotApprovedException() {
        super("KYC approval is required before transferring funds");
    }
}
