package com.openqualityhub.service.webhook;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class HmacValidator {

    private static final String HMAC_SHA256 = "HmacSHA256";

    public boolean validate(String payload, String secret, String signature) {
        if (secret == null || secret.isBlank() || signature == null || signature.isBlank()) {
            return false;
        }

        try {
            String computed = computeHmacSha256(payload, secret);

            // Support GitHub-style "sha256=..." prefix
            String cleanSignature = signature.startsWith("sha256=")
                    ? signature.substring(7)
                    : signature;

            return MessageDigest.isEqual(
                    computed.getBytes(StandardCharsets.UTF_8),
                    cleanSignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }

    private String computeHmacSha256(String data, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
