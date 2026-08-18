package com.innerderma.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * 대회 시연용 자체 JWT 발급/검증. 회원가입 없이 userCode로 토큰 발급.
 */
@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final Duration tokenValidity;

    public JwtProvider(
            @Value("${jwt.secret:innerderma-demo-secret-key-must-be-at-least-32-bytes}") String secret,
            @Value("${jwt.validity-hours:24}") int validityHours
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.tokenValidity = Duration.ofHours(validityHours);
    }

    public String generateToken(String userCode) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userCode)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(tokenValidity)))
                .signWith(secretKey)
                .compact();
    }

    public String extractUserCode(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public boolean isValid(String token) {
        try {
            extractUserCode(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }
}
