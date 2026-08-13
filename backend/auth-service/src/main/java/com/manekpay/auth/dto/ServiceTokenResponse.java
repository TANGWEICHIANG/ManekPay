package com.manekpay.auth.dto;

public record ServiceTokenResponse(String accessToken, long expiresIn) {
}
