package com.manekpay.fx.exception;

public class RateNotFoundException extends RuntimeException {
    public RateNotFoundException(String pair) {
        super("No rate available for " + pair);
    }
}
