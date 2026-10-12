package com.techmart.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtTokenProvider(
            @Value("${techmart.jwt.secret:techmart_super_secure_jwt_secret_key_2026_at_least_256_bits_long!}") String secret,
            @Value("${techmart.jwt.expiration-seconds:900}") long expirationSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[JwtTokenProvider][validateToken] Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        String subject = claims.getSubject();
        if (subject != null) {
            return UUID.fromString(subject);
        }
        String userIdClaim = claims.get("userId", String.class);
        return UUID.fromString(userIdClaim);
    }

    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("email", String.class);
    }

    public String generateAccessToken(UUID userId, String email, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + (expirationSeconds * 1000));

        return Jwts.builder()
                .subject(userId.toString())
                .claim("userId", userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }
}
