package com.manekpay.ledger.exception;

public class RiskServiceUnavailableException extends RuntimeException {
    public RiskServiceUnavailableException(Throwable cause) {
        super("Could not verify account risk status - please try again shortly", cause);
    }
}
