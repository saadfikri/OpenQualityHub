package com.openqualityhub.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLW9ubHktbXVzdC1iZS0zMi1ieXRlcw==";
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String EMAIL = "test@example.com";
    private static final String ORG_ID = "00000000-0000-0000-0000-000000000001";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 86400000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpirationMs", 604800000L);
        jwtService.initSigningKey();
    }

    @Test
    void generateAccessToken_shouldReturnNonEmptyToken() {
        String token = jwtService.generateAccessToken(USER_ID, EMAIL, ORG_ID, Set.of("TEST_READ"));
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void generateAccessToken_shouldContainCorrectClaims() {
        Set<String> permissions = Set.of("TEST_READ", "TEST_WRITE");
        String token = jwtService.generateAccessToken(USER_ID, EMAIL, ORG_ID, permissions);

        Claims claims = jwtService.extractClaims(token);
        assertEquals(USER_ID.toString(), claims.getSubject());
        assertEquals(EMAIL, claims.get("email"));
        assertEquals(ORG_ID, claims.get("orgId"));
        assertNotNull(claims.get("permissions"));
    }

    @Test
    void generateRefreshToken_shouldContainUserId() {
        String token = jwtService.generateRefreshToken(USER_ID);
        UUID extracted = jwtService.extractUserId(token);
        assertEquals(USER_ID, extracted);
    }

    @Test
    void extractUserId_shouldReturnCorrectUserId() {
        String token = jwtService.generateAccessToken(USER_ID, EMAIL, ORG_ID, Set.of());
        assertEquals(USER_ID, jwtService.extractUserId(token));
    }

    @Test
    void isTokenValid_shouldReturnTrueForValidToken() {
        String token = jwtService.generateAccessToken(USER_ID, EMAIL, ORG_ID, Set.of());
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void isTokenValid_shouldReturnFalseForExpiredToken() {
        // Set expiration to -1ms (already expired)
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", -1000L);
        String token = jwtService.generateAccessToken(USER_ID, EMAIL, ORG_ID, Set.of());
        assertFalse(jwtService.isTokenValid(token));
    }

    @Test
    void isTokenValid_shouldReturnFalseForGarbageToken() {
        assertFalse(jwtService.isTokenValid("not.a.valid.jwt"));
    }

    @Test
    void isTokenValid_shouldReturnFalseForTamperedToken() {
        String token = jwtService.generateAccessToken(USER_ID, EMAIL, ORG_ID, Set.of());
        // Tamper with payload
        String tampered = token.substring(0, token.lastIndexOf('.')) + ".invalidsignature";
        assertFalse(jwtService.isTokenValid(tampered));
    }

    @Test
    void isTokenValid_shouldReturnFalseForTokenSignedWithDifferentKey() {
        String token = jwtService.generateAccessToken(USER_ID, EMAIL, ORG_ID, Set.of());

        // Create a new service with a different key
        JwtService otherService = new JwtService();
        ReflectionTestUtils.setField(otherService, "jwtSecret",
            "YW5vdGhlci1zZWNyZXQta2V5LWZvci10ZXN0aW5nLXB1cnBvc2VzLW11c3QtYmUtMzItYnl0ZXM=");
        ReflectionTestUtils.setField(otherService, "jwtExpirationMs", 86400000L);
        ReflectionTestUtils.setField(otherService, "refreshExpirationMs", 604800000L);
        otherService.initSigningKey();

        assertFalse(otherService.isTokenValid(token));
    }
}
