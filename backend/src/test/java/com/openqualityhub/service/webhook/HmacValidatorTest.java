package com.openqualityhub.service.webhook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class HmacValidatorTest {

    private HmacValidator validator;

    private static final String SECRET = "test-webhook-secret";
    private static final String PAYLOAD = "{\"action\":\"completed\",\"workflow_run\":{\"id\":123}}";

    @BeforeEach
    void setUp() {
        validator = new HmacValidator();
    }

    @Test
    void validate_shouldReturnTrueForValidSignature() throws Exception {
        String signature = computeHmac(PAYLOAD, SECRET);
        assertTrue(validator.validate(PAYLOAD, SECRET, signature));
    }

    @Test
    void validate_shouldReturnTrueForGitHubStylePrefixedSignature() throws Exception {
        String signature = "sha256=" + computeHmac(PAYLOAD, SECRET);
        assertTrue(validator.validate(PAYLOAD, SECRET, signature));
    }

    @Test
    void validate_shouldReturnFalseForInvalidSignature() {
        assertFalse(validator.validate(PAYLOAD, SECRET, "invalidsignaturehex"));
    }

    @Test
    void validate_shouldReturnFalseForWrongSecret() throws Exception {
        String signature = computeHmac(PAYLOAD, "wrong-secret");
        assertFalse(validator.validate(PAYLOAD, SECRET, signature));
    }

    @Test
    void validate_shouldReturnFalseForTamperedPayload() throws Exception {
        String signature = computeHmac(PAYLOAD, SECRET);
        assertFalse(validator.validate(PAYLOAD + "tampered", SECRET, signature));
    }

    @Test
    void validate_shouldReturnFalseForNullSecret() {
        assertFalse(validator.validate(PAYLOAD, null, "somesig"));
    }

    @Test
    void validate_shouldReturnFalseForBlankSecret() {
        assertFalse(validator.validate(PAYLOAD, "  ", "somesig"));
    }

    @Test
    void validate_shouldReturnFalseForNullSignature() {
        assertFalse(validator.validate(PAYLOAD, SECRET, null));
    }

    @Test
    void validate_shouldReturnFalseForBlankSignature() {
        assertFalse(validator.validate(PAYLOAD, SECRET, "  "));
    }

    private String computeHmac(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
