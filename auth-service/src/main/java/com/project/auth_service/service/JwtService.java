package com.project.auth_service.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

/**
 * Service for managing JSON Web Tokens (JWT).
 * Handles token generation, signing, and parsing for authentication and authorization.
 */
@Component
public class JwtService {
    private Key signingKey;

    @Value("${app.secret:}")
    private String secret;

    @Value("${jwt.access.expiration:900000}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh.expiration:604800000}")
    private long refreshTokenExpiration;

    /**
     * Initializes the signing key using the configured secret.
     * Fails fast if the secret is missing or too short for secure HMAC-SHA signing.
     */
    @PostConstruct
    public void init() {
        if (secret == null || secret.trim().isEmpty() || secret.length() < 32) {
            throw new IllegalStateException("JWT Secret is missing or too short. Failing fast in production config.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a signed access token for a user.
     * 
     * @param username The name of the user
     * @param userId The unique identifier of the user (set as token subject)
     * @param roles Set of roles assigned to the user
     * @return A signed JWT access token string
     */
    public String generateAccessToken(String username, String userId, java.util.Set<com.project.auth_service.entity.Role> roles) {
        java.util.List<String> roleNames = roles.stream().map(Enum::name).collect(java.util.stream.Collectors.toList());
        return Jwts.builder()
                .setSubject(userId)
                .claim("preferred_username", username)
                .claim("roles", roleNames)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generates a unique refresh token string.
     * 
     * @return A random UUID-based string to be used as a refresh token
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
    }

    /**
     * Gets the configured expiration time for access tokens.
     * 
     * @return Expiration time in milliseconds
     */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    /**
     * Gets the configured expiration time for refresh tokens.
     * 
     * @return Expiration time in milliseconds
     */
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    /**
     * Parses and validates a JWT token, returning its claims.
     * 
     * @param token The JWT token string to parse
     * @return The extracted Claims from the token
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
