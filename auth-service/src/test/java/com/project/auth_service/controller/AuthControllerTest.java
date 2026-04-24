package com.project.auth_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.auth_service.constants.Endpoints;
import com.project.auth_service.dto.*;
import com.project.auth_service.entity.RefreshToken;
import com.project.auth_service.entity.Role;
import com.project.auth_service.entity.User;
import com.project.auth_service.helper.AuthValidator;
import com.project.auth_service.repository.RefreshTokenRepository;
import com.project.auth_service.repository.UserRepository;
import com.project.auth_service.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthValidator authValidator;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        AuthController authController = new AuthController(jwtService, userRepository, refreshTokenRepository, passwordEncoder, authValidator);
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    void register_ShouldReturnOk_WhenSuccess() throws Exception {
        RegisterRequestDto dto = new RegisterRequestDto();
        dto.setName("testuser");
        dto.setPassword("Password123!");
        dto.setEmail("test@example.com");

        User user = new User();
        user.setName("testuser");
        user.setId(java.util.UUID.randomUUID());
        user.setRoles(Collections.singleton(Role.PATIENT));

        RefreshToken rf = new RefreshToken();
        rf.setToken("refresh-token");

        when(authValidator.checkIfUsernameAlreadyExistsOrNotForRegisterMethod(any(), any())).thenReturn(null);
        when(authValidator.registerRequestDtoToUserForRegisterMethod(any(), any())).thenReturn(user);
        when(userRepository.save(any())).thenReturn(user);
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken()).thenReturn("refresh-token");
        when(refreshTokenRepository.save(any())).thenReturn(rf);

        mockMvc.perform(post(Endpoints.AUTH_CONTROLLER_REQUEST + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void login_ShouldReturnOk_WhenSuccess() throws Exception {
        LoginRequestDto dto = new LoginRequestDto();
        dto.setName("testuser");
        dto.setPassword("Password123!");

        User user = new User();
        user.setName("testuser");
        user.setId(java.util.UUID.randomUUID());
        user.setRoles(Collections.singleton(Role.PATIENT));

        RefreshToken rf = new RefreshToken();
        rf.setToken("refresh-token");

        when(userRepository.findByName("testuser")).thenReturn(Optional.of(user));
        when(authValidator.checkIfUsernameOrPasswordIsEmptyForLoginMethod(any())).thenReturn(null);
        when(authValidator.checkIfPasswordIsInvalidForLogin(any(), any(), any())).thenReturn(null);
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken()).thenReturn("refresh-token");
        when(refreshTokenRepository.save(any())).thenReturn(rf);

        mockMvc.perform(post(Endpoints.AUTH_CONTROLLER_REQUEST + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void refresh_ShouldReturnOk_WhenTokenIsValid() throws Exception {
        RefreshTokenRequestDto dto = new RefreshTokenRequestDto();
        dto.setRefreshToken("valid-refresh-token");

        User user = new User();
        user.setName("testuser");
        user.setId(java.util.UUID.randomUUID());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("valid-refresh-token");
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(3600));
        refreshToken.setRevoked(false);

        RefreshToken rf = new RefreshToken();
        rf.setToken("new-refresh-token");

        when(refreshTokenRepository.findByToken("valid-refresh-token")).thenReturn(Optional.of(refreshToken));
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken()).thenReturn("new-refresh-token");
        when(refreshTokenRepository.save(any())).thenReturn(rf);

        mockMvc.perform(post(Endpoints.AUTH_CONTROLLER_REQUEST + "/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
    }

    @Test
    void logout_ShouldReturnOk_WhenTokenExists() throws Exception {
        LogoutRequestDto dto = new LogoutRequestDto();
        dto.setRefreshToken("valid-refresh-token");

        User user = new User();
        user.setId(UUID.randomUUID());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("valid-refresh-token");
        refreshToken.setUser(user);

        when(refreshTokenRepository.findByToken("valid-refresh-token")).thenReturn(Optional.of(refreshToken));

        mockMvc.perform(post(Endpoints.AUTH_CONTROLLER_REQUEST + "/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("Log out successful"));

        verify(refreshTokenRepository, times(1)).save(any());
    }
}
