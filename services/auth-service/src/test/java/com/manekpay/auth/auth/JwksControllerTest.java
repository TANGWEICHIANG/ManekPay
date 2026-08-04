package com.manekpay.auth.auth;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwksControllerTest {

    @Test
    void jwksModulusAndExponentAreRfc7518CompliantAndReconstructTheSameKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        RSAPublicKey originalKey = (RSAPublicKey) pair.getPublic();

        JwksController controller = new JwksController(originalKey);
        Map<String, Object> jwks = controller.jwks();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");
        assertThat(keys).hasSize(1);
        Map<String, Object> key = keys.get(0);

        String n = (String) key.get("n");
        String e = (String) key.get("e");

        byte[] nBytes = Base64.getUrlDecoder().decode(n);
        byte[] eBytes = Base64.getUrlDecoder().decode(e);

        // RFC 7518 §6.3.1.1: no leading zero byte on a value whose high bit is set.
        assertThat(nBytes[0]).isNotEqualTo((byte) 0);

        // Reconstruct the public key from the JWKS n/e and confirm it round-trips to the
        // exact same key - this is the real test a strict JOSE library implicitly performs.
        BigInteger modulus = new BigInteger(1, nBytes);
        BigInteger exponent = new BigInteger(1, eBytes);
        RSAPublicKey reconstructed = (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new RSAPublicKeySpec(modulus, exponent));

        assertThat(reconstructed.getModulus()).isEqualTo(originalKey.getModulus());
        assertThat(reconstructed.getPublicExponent()).isEqualTo(originalKey.getPublicExponent());
    }
}
