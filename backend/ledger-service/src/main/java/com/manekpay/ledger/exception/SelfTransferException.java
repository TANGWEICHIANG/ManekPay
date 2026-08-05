package com.manekpay.ledger.exception;

public class SelfTransferException extends RuntimeException {
    public SelfTransferException() {
        super("Cannot transfer to your own account");
    }
}
