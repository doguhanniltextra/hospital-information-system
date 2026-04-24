package com.project.auth_service.service;

import com.project.auth_service.entity.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = "myVeryLongAndSecureSecretKeyForTestingPurposes123!";
    private final long accessExpiration = 900000;
    private final long refreshExpiration = 604800000;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", secret);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", accessExpiration);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", refreshExpiration);
        jwtService.init();
    }

    @Test
    void generateAccessToken_ShouldCreateValidToken() {
        String username = "testuser";
        String userId = "user-123";
        Set<Role> roles = new HashSet<>(Collections.singletonList(Role.ADMIN));

        String token = jwtService.generateAccessToken(username, userId, roles);

        assertNotNull(token);
        Claims claims = jwtService.parseToken(token);
        assertEquals(userId, claims.getSubject());
        assertEquals(username, claims.get("preferred_username"));
        assertNotNull(claims.get("roles"));
    }

    @Test
    void generateRefreshToken_ShouldCreateUniqueString() {
        String token1 = jwtService.generateRefreshToken();
        String token2 = jwtService.generateRefreshToken();

        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
        assertTrue(token1.contains("-"));
    }

    @Test
    void getExpirations_ShouldReturnConfiguredValues() {
        assertEquals(accessExpiration, jwtService.getAccessTokenExpiration());
        assertEquals(refreshExpiration, jwtService.getRefreshTokenExpiration());
    }

    @Test
    void parseToken_ShouldThrowExceptionForInvalidToken() {
        String invalidToken = "invalid.token.here";
        assertThrows(Exception.class, () -> jwtService.parseToken(invalidToken));
    }
}
