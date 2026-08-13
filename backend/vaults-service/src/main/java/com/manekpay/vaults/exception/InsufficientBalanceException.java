package com.manekpay.vaults.exception;

// Mirrors ledger-service's own exception of the same name, but this is vaults-service's local
// translation of ledger-service's 422 response - it is never itself serialized to an HTTP
// response (SweepScheduler catches it internally), so it does not need an ApiExceptionHandler entry.
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException() {
        super("Insufficient balance for sweep");
    }
}
