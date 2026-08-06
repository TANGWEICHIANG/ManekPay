package com.manekpay.wealth.exception;

public class AssetNotFoundException extends RuntimeException {
    public AssetNotFoundException() {
        super("Asset not found");
    }
}
