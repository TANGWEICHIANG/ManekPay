package com.manekpay.ledger.exception;

public class FxServiceUnavailableException extends RuntimeException {
    public FxServiceUnavailableException(Throwable cause) {
        super("Could not retrieve exchange rate - please try again shortly", cause);
    }
}
