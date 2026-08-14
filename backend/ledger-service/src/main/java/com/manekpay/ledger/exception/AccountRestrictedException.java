package com.manekpay.ledger.exception;

import java.time.Instant;

public class AccountRestrictedException extends RuntimeException {
    public AccountRestrictedException(Instant restrictedUntil) {
        super("Your account is temporarily restricted from sending money until " + restrictedUntil);
    }
}
