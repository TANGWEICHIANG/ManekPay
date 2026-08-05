package com.manekpay.ledger.exception;

public class ProxyNotFoundException extends RuntimeException {
    public ProxyNotFoundException() {
        super("Proxy not found");
    }
}
