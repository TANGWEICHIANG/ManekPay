package com.manekpay.auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
public class JwksController {

    private final RSAPublicKey publicKey;

    public JwksController(RSAPublicKey publicKey) {
        this.publicKey = publicKey;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        String n = Base64.getUrlEncoder().withoutPadding().encodeToString(unsignedBytes(publicKey.getModulus().toByteArray()));
        String e = Base64.getUrlEncoder().withoutPadding().encodeToString(unsignedBytes(publicKey.getPublicExponent().toByteArray()));
        Map<String, Object> key = Map.of(
                "kty", "RSA",
                "use", "sig",
                "alg", "RS256",
                "kid", "auth-service-key-1",
                "n", n,
                "e", e
        );
        return Map.of("keys", List.of(key));
    }

    // RFC 7518 §6.3.1.1: the encoded octet sequence MUST NOT have a leading zero byte,
    // but BigInteger.toByteArray() (signed two's-complement) prepends one whenever the
    // value's high bit is set - strip it so the JWKS modulus/exponent are minimal-length.
    private static byte[] unsignedBytes(byte[] signed) {
        if (signed.length > 1 && signed[0] == 0) {
            byte[] trimmed = new byte[signed.length - 1];
            System.arraycopy(signed, 1, trimmed, 0, trimmed.length);
            return trimmed;
        }
        return signed;
    }
}
