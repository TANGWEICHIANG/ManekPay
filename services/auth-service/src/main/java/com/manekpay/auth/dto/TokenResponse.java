package com.manekpay.auth.dto;

public record TokenResponse(String accessToken, String refreshToken, long expiresIn) {
}
