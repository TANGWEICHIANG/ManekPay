package com.manekpay.vaults.exception;

public class VaultNotFoundException extends RuntimeException {
    public VaultNotFoundException() {
        super("No vault exists yet for this customer");
    }
}
