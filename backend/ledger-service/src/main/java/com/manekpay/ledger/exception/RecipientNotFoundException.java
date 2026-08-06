package com.manekpay.ledger.exception;

public class RecipientNotFoundException extends RuntimeException {
    public RecipientNotFoundException() {
        super("Recipient not found");
    }
}
