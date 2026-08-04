package com.manekpay.auth.auth;

public record TokenResponse(String accessToken, String refreshToken, long expiresIn) {
}
