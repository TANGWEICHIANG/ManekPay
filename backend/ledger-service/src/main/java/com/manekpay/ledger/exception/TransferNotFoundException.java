package com.manekpay.ledger.exception;

public class TransferNotFoundException extends RuntimeException {
    public TransferNotFoundException() {
        super("Transfer not found");
    }
}
