package com.company.mts.security;

import com.company.mts.entity.AuthUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private AuthUser testUser;
    private final String secret = "MySuperSecretKeyForJWTTokenGenerationAndValidationInMoneyTransferSystem2024";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", secret);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 86400000); // 1 day

        testUser = new AuthUser();
        testUser.setId(123L);
        testUser.setName("alice");
        testUser.setEmail("alice@test.com");
    }

    @Test
    void tokenLifecycle_Success() {
        String token = jwtTokenProvider.generateToken(testUser);
        assertNotNull(token);

        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("alice", jwtTokenProvider.getUsernameFromToken(token));
        assertEquals(123L, jwtTokenProvider.getUserIdFromToken(token));
        assertEquals("alice@test.com", jwtTokenProvider.getEmailFromToken(token));
    }

    @Test
    void parseToken_InvalidToken_ReturnsNull() {
        String badToken = "invalidHeader.invalidPayload.invalidSignature";
        assertNull(jwtTokenProvider.getUsernameFromToken(badToken));
        assertNull(jwtTokenProvider.getUserIdFromToken(badToken));
        assertNull(jwtTokenProvider.getEmailFromToken(badToken));
    }

    @Test
    void validateToken_MalformedToken_ReturnsFalse() {
        assertFalse(jwtTokenProvider.validateToken("not.a.token"));
    }

    @Test
    void validateToken_EmptyToken_ReturnsFalse() {
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken(null));
    }

    @Test
    void validateToken_ExpiredToken_ReturnsFalse() {
        // Set short expiration
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", -1000);
        String expiredToken = jwtTokenProvider.generateToken(testUser);

        assertFalse(jwtTokenProvider.validateToken(expiredToken));
    }

    @Test
    void validateToken_UnsupportedToken_ReturnsFalse() {
        // Create token without signature (plaintext JWT)
        String unsignedToken = Jwts.builder()
                .subject("test")
                .compact();

        assertFalse(jwtTokenProvider.validateToken(unsignedToken));
    }
}
