package com.manekpay.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class JwtKeyConfig {

    @Value("${app.jwt.private-key}")
    private String privateKeyBase64;

    @Value("${app.jwt.public-key}")
    private String publicKeyBase64;

    @Bean
    public RSAPrivateKey jwtPrivateKey() throws Exception {
        byte[] bytes = Base64.getDecoder().decode(privateKeyBase64);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    @Bean
    public RSAPublicKey jwtPublicKey() throws Exception {
        byte[] bytes = Base64.getDecoder().decode(publicKeyBase64);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(bytes));
    }
}
