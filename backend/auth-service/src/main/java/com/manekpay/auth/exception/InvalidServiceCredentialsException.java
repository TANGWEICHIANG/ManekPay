package com.manekpay.auth.exception;

public class InvalidServiceCredentialsException extends RuntimeException {
    public InvalidServiceCredentialsException() {
        super("Invalid client id or secret");
    }
}
