package com.manekpay.fx.exception;

public class LockNotFoundException extends RuntimeException {
    public LockNotFoundException() {
        super("Rate lock not found or expired");
    }
}
