package com.manekpay.auth.config;

import java.time.Instant;

public record ErrorResponse(Instant timestamp, int status, String error, String message, String path) {
}
