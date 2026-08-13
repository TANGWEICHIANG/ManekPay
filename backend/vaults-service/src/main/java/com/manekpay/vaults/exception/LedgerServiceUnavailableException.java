package com.manekpay.vaults.exception;

public class LedgerServiceUnavailableException extends RuntimeException {
    public LedgerServiceUnavailableException(Throwable cause) {
        super("ledger-service unreachable", cause);
    }
}
