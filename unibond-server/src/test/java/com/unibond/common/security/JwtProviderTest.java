package com.unibond.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtProviderTest {
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(
            "test-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm",
            7200000L,
            2592000000L
        );
    }

    @Test
    void createAndValidateAccessToken() {
        String token = jwtProvider.createAccessToken(1L);
        assertTrue(jwtProvider.validate(token));
        assertEquals(1L, jwtProvider.getUserId(token));
    }

    @Test
    void createAndValidateRefreshToken() {
        String token = jwtProvider.createRefreshToken(1L);
        assertTrue(jwtProvider.validate(token));
        assertEquals(1L, jwtProvider.getUserId(token));
    }

    @Test
    void invalidTokenReturnsFalse() {
        assertFalse(jwtProvider.validate("invalid.token.here"));
    }
}
