package com.project.auth_service.controller;

import com.project.auth_service.constants.Endpoints;
import com.project.auth_service.constants.LogMessages;
import com.project.auth_service.dto.*;
import com.project.auth_service.entity.PasswordResetToken;
import com.project.auth_service.entity.RefreshToken;
import com.project.auth_service.entity.User;
import com.project.auth_service.helper.AuthValidator;
import com.project.auth_service.repository.PasswordResetTokenRepository;
import com.project.auth_service.repository.RefreshTokenRepository;
import com.project.auth_service.repository.UserRepository;
import com.project.auth_service.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

import jakarta.validation.Valid;

/**
 * REST controller for authentication and authorization operations.
 * Handles user registration, login, token refresh, logout, and password setting.
 */
@RestController
@RequestMapping(Endpoints.AUTH_CONTROLLER_REQUEST)
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthValidator authValidator;
    private final PasswordResetTokenRepository resetTokenRepository;

    /**
     * Initializes the AuthController with required dependencies.
     * 
     * @param jwtService Service for JWT token generation and parsing
     * @param userRepository Repository for user data
     * @param refreshTokenRepository Repository for refresh tokens
     * @param passwordEncoder Encoder for secure password storage
     * @param authValidator Helper for validating authentication requests
     * @param resetTokenRepository Repository for password reset tokens
     */
    public AuthController(JwtService jwtService, UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder,
            AuthValidator authValidator, PasswordResetTokenRepository resetTokenRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authValidator = authValidator;
        this.resetTokenRepository = resetTokenRepository;
    }

    /**
     * Creates and saves a new refresh token for a user.
     * 
     * @param user The user to create the token for
     * @return The saved RefreshToken entity
     */
    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(jwtService.generateRefreshToken());
        refreshToken.setExpiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpiration()));
        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Registers a new standard user.
     * 
     * @param registerRequestDto Data transfer object containing registration details
     * @return ResponseEntity containing success message and tokens, or error message
     */
    @PostMapping(path = Endpoints.REGISTER, produces = Endpoints.PRODUCES)
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDto registerRequestDto) {

        log.info(LogMessages.REGISTER_METHOD_TRIGGERED);
        ResponseEntity<String> usernameCheckResult = authValidator
                .checkIfUsernameAlreadyExistsOrNotForRegisterMethod(registerRequestDto, userRepository);
        if (usernameCheckResult != null)
            return usernameCheckResult;
        log.info(LogMessages.REGISTER_USERNAME_EXISTS);
        User user = authValidator.registerRequestDtoToUserForRegisterMethod(registerRequestDto, passwordEncoder);

        userRepository.save(user);
        log.info(LogMessages.REGISTER_USER_SAVED);

        String accessToken = jwtService.generateAccessToken(user.getName(), user.getId().toString(), user.getRoles());
        RefreshToken refreshTokenObj = createRefreshToken(user);

        log.info(LogMessages.REGISTER_TOKEN_GENERATED);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterResponseDto("User registered successfully", accessToken, refreshTokenObj.getToken(),
                        jwtService.getAccessTokenExpiration() / 1000));
    }

    /**
     * Registers a new administrative/privileged user.
     * 
     * @param registerRequestDto Data transfer object containing registration details
     * @return ResponseEntity containing success message and tokens, or error message
     */
    @PostMapping(path = Endpoints.ADMIN_REGISTER, produces = Endpoints.PRODUCES)
    public ResponseEntity<?> adminRegister(@Valid @RequestBody AdminRegisterRequestDto registerRequestDto) {

        log.info(LogMessages.REGISTER_METHOD_TRIGGERED);
        ResponseEntity<String> usernameCheckResult = authValidator
                .checkIfUsernameAlreadyExistsOrNotForRegisterMethod(registerRequestDto, userRepository);
        if (usernameCheckResult != null)
            return usernameCheckResult;
        log.info(LogMessages.REGISTER_USERNAME_EXISTS);
        User user = authValidator.adminRegisterRequestDtoToUser(registerRequestDto, passwordEncoder);

        userRepository.save(user);
        log.info(LogMessages.REGISTER_USER_SAVED);

        String accessToken = jwtService.generateAccessToken(user.getName(), user.getId().toString(), user.getRoles());
        RefreshToken refreshTokenObj = createRefreshToken(user);

        log.info(LogMessages.REGISTER_TOKEN_GENERATED);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterResponseDto("Privileged user registered successfully", accessToken,
                        refreshTokenObj.getToken(),
                        jwtService.getAccessTokenExpiration() / 1000));
    }

    /**
     * Authenticates a user and generates access and refresh tokens.
     * 
     * @param loginRequestDto Data transfer object containing login credentials
     * @return ResponseEntity containing success message and tokens, or error message
     */
    @PostMapping(path = Endpoints.LOGIN, produces = Endpoints.PRODUCES)
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequestDto) {
        log.info(LogMessages.LOGIN_METHOD_TRIGGERED);
        User user = userRepository.findByName(loginRequestDto.getName())
                .orElse(null);

        ResponseEntity<String> checkUsernameOrPassword = authValidator
                .checkIfUsernameOrPasswordIsEmptyForLoginMethod(user);
        if (checkUsernameOrPassword != null)
            return checkUsernameOrPassword;
        log.info(LogMessages.LOGIN_USERNAME_NOT_FOUND);

        ResponseEntity<String> passwordCheckResult = authValidator.checkIfPasswordIsInvalidForLogin(loginRequestDto,
                user, passwordEncoder);
        if (passwordCheckResult != null)
            return passwordCheckResult;
        log.info(LogMessages.LOGIN_INVALID_CREDENTIALS);

        String accessToken = jwtService.generateAccessToken(user.getName(), user.getId().toString(), user.getRoles());
        RefreshToken refreshTokenObj = createRefreshToken(user);

        log.info(LogMessages.LOGIN_TOKEN_GENERATED);

        return ResponseEntity.ok(new LoginResponseDto(LogMessages.LOGIN_SUCCESS, accessToken,
                refreshTokenObj.getToken(), jwtService.getAccessTokenExpiration() / 1000));
    }

    /**
     * Refreshes an expired access token using a valid refresh token.
     * Revokes the old refresh token and issues a new one.
     * 
     * @param requestDto Data transfer object containing the refresh token
     * @return ResponseEntity containing new tokens, or error message
     */
    @PostMapping(path = Endpoints.REFRESH, produces = Endpoints.PRODUCES)
    public ResponseEntity<?> refresh(@RequestBody RefreshTokenRequestDto requestDto) {
        String requestToken = requestDto.getRefreshToken();

        return refreshTokenRepository.findByToken(requestToken)
                .map(token -> {
                    if (token.getExpiresAt().compareTo(Instant.now()) < 0) {
                        refreshTokenRepository.delete(token);
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body("Refresh token was expired. Please make a new signin request");
                    }
                    if (token.isRevoked()) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token has been revoked.");
                    }

                    // Revoke old token
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);

                    // Generate new access and refresh token
                    User user = token.getUser();
                    String accessToken = jwtService.generateAccessToken(user.getName(), user.getId().toString(),
                            user.getRoles());
                    RefreshToken newRefreshToken = createRefreshToken(user);

                    return ResponseEntity.ok(new LoginResponseDto("Token refreshed successfully", accessToken,
                            newRefreshToken.getToken(), jwtService.getAccessTokenExpiration() / 1000));
                })
                .orElseGet(
                        () -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token is not in database!"));
    }

    /**
     * Logs out a user by revoking their refresh token.
     * 
     * @param requestDto Data transfer object containing the refresh token to revoke
     * @return ResponseEntity with a success message
     */
    @PostMapping(path = Endpoints.LOGOUT, produces = Endpoints.PRODUCES)
    public ResponseEntity<?> logout(@RequestBody LogoutRequestDto requestDto) {
        String requestToken = requestDto.getRefreshToken();
        refreshTokenRepository.findByToken(requestToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            log.info("Refresh token revoked for user {}", token.getUser().getId());
        });
        return ResponseEntity.ok().body("Log out successful");
    }

    /**
     * Allows auto-provisioned PATIENT accounts to activate their account by setting a real password.
     * The token is a one-time UUID sent via the welcome email. Token is valid for 24 hours.
     */
    @PostMapping(path = Endpoints.SET_PASSWORD, produces = Endpoints.PRODUCES)
    public ResponseEntity<?> setPassword(@Valid @RequestBody SetPasswordRequestDto requestDto) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(requestDto.getToken())
                .orElse(null);

        if (resetToken == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired reset token.");
        }
        if (resetToken.isUsed()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Reset token has already been used.");
        }
        if (resetToken.getExpiresAt().isBefore(java.time.Instant.now())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Reset token has expired. Please contact the clinic.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(requestDto.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);

        log.info("SetPassword: Account activated for userId={}", user.getId());

        String accessToken = jwtService.generateAccessToken(user.getName(), user.getId().toString(), user.getRoles());
        RefreshToken refreshTokenObj = createRefreshToken(user);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginResponseDto("Password set successfully. Welcome!", accessToken,
                        refreshTokenObj.getToken(), jwtService.getAccessTokenExpiration() / 1000));
    }
}
