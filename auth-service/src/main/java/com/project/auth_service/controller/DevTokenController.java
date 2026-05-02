package com.project.auth_service.controller;

import com.project.auth_service.dto.LoginResponseDto;
import com.project.auth_service.entity.Role;
import com.project.auth_service.service.JwtService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Profile("dev")
@RestController
@RequestMapping("/auth/dev")
public class DevTokenController {

    private final JwtService jwtService;

    public DevTokenController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping(path = "/token", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponseDto> token(
            @RequestParam(defaultValue = "PATIENT") Role role,
            @RequestParam(defaultValue = "readme-user") String username) {
        String accessToken = generateToken(role, username);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginResponseDto("Development token generated", accessToken, null,
                        jwtService.getAccessTokenExpiration() / 1000));
    }

    @GetMapping(path = "/token/raw", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> rawToken(
            @RequestParam(defaultValue = "PATIENT") Role role,
            @RequestParam(defaultValue = "readme-user") String username) {
        return ResponseEntity.ok(generateToken(role, username));
    }

    private String generateToken(Role role, String username) {
        Set<Role> roles = EnumSet.of(role);
        return jwtService.generateAccessToken(username, UUID.randomUUID().toString(), roles);
    }
}
