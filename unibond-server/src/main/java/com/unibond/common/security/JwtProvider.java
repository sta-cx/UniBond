package com.unibond.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtProvider {
    private final SecretKey key;
    private final long accessExpiry;
    private final long refreshExpiry;

    public JwtProvider(String secret, long accessExpiry, long refreshExpiry) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiry = accessExpiry;
        this.refreshExpiry = refreshExpiry;
    }

    public String createAccessToken(Long userId) {
        return buildToken(userId, accessExpiry, "access");
    }

    public String createRefreshToken(Long userId) {
        return buildToken(userId, refreshExpiry, "refresh");
    }

    private String buildToken(Long userId, long expiry, String type) {
        Date now = new Date();
        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", type)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expiry))
            .signWith(key)
            .compact();
    }

    public boolean validate(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    public String getType(String token) {
        return getClaims(token).get("type", String.class);
    }

    private Claims getClaims(String token) {
        return Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).getPayload();
    }
}
