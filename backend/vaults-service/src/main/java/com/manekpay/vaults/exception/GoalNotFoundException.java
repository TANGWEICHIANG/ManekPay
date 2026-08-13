package com.manekpay.vaults.exception;

public class GoalNotFoundException extends RuntimeException {
    public GoalNotFoundException() {
        super("No goal exists with this id for this customer");
    }
}
