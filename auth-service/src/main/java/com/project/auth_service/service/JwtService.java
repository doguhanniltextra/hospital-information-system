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

@Component
public class JwtService {
    private Key signingKey;

    @Value("${app.secret:}")
    private String secret;

    @Value("${jwt.access.expiration:900000}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh.expiration:604800000}")
    private long refreshTokenExpiration;

    @PostConstruct
    public void init() {
        if (secret == null || secret.trim().isEmpty() || secret.length() < 32) {
            throw new IllegalStateException("JWT Secret is missing or too short. Failing fast in production config.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

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

    public String generateRefreshToken() {
        return UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
