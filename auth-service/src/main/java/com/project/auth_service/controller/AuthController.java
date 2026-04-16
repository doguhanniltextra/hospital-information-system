package com.project.auth_service.controller;

import com.project.auth_service.constants.Endpoints;
import com.project.auth_service.constants.LogMessages;
import com.project.auth_service.dto.*;
import com.project.auth_service.entity.RefreshToken;
import com.project.auth_service.entity.User;
import com.project.auth_service.helper.AuthValidator;
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

@RestController
@RequestMapping(Endpoints.AUTH_CONTROLLER_REQUEST)
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthValidator authValidator;

    public AuthController(JwtService jwtService, UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder, AuthValidator authValidator) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authValidator = authValidator;
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(jwtService.generateRefreshToken());
        refreshToken.setExpiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpiration()));
        return refreshTokenRepository.save(refreshToken);
    }

    @PostMapping(path = Endpoints.REGISTER, produces = Endpoints.PRODUCES)
    public ResponseEntity<?> register(@RequestBody RegisterRequestDto registerRequestDto) {

        log.info(LogMessages.REGISTER_METHOD_TRIGGERED);
        ResponseEntity<String> Username_already_exists = authValidator.checkIfUsernameAlreadyExistsOrNotForRegisterMethod(registerRequestDto, userRepository);
        if (Username_already_exists != null) return Username_already_exists;
        log.info(LogMessages.REGISTER_USERNAME_EXISTS);
        User user = authValidator.registerRequestDtoToUserForRegisterMethod(registerRequestDto, passwordEncoder);

        userRepository.save(user);
        log.info(LogMessages.REGISTER_USER_SAVED);
        
        String accessToken = jwtService.generateAccessToken(user.getName(), user.getId().toString(), user.getRoles());
        RefreshToken refreshTokenObj = createRefreshToken(user);
        
        log.info(LogMessages.REGISTER_TOKEN_GENERATED);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterResponseDto("User registered successfully", accessToken, refreshTokenObj.getToken(), jwtService.getAccessTokenExpiration() / 1000));
    }


    @PostMapping(path = Endpoints.LOGIN, produces = Endpoints.PRODUCES)
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequestDto) {
        log.info(LogMessages.LOGIN_METHOD_TRIGGERED);
        User user = userRepository.findByName(loginRequestDto.getName())
                .orElse(null);

        ResponseEntity<String> checkUsernameOrPassword = authValidator.checkIfUsernameOrPasswordIsEmptyForLoginMethod(user);
        if (checkUsernameOrPassword != null) return checkUsernameOrPassword;
        log.info(LogMessages.LOGIN_USERNAME_NOT_FOUND);

        ResponseEntity<String> CheckPasswordEncoder = authValidator.CheckIfUsernameOrPasswordIsInvalidForPasswordEncoderForLoginMethod(loginRequestDto, user, passwordEncoder);
        if (CheckPasswordEncoder != null) return CheckPasswordEncoder;
        log.info(LogMessages.LOGIN_INVALID_CREDENTIALS);
        
        String accessToken = jwtService.generateAccessToken(user.getName(), user.getId().toString(), user.getRoles());
        RefreshToken refreshTokenObj = createRefreshToken(user);
        
        log.info(LogMessages.LOGIN_TOKEN_GENERATED);

        return ResponseEntity.ok(new LoginResponseDto(LogMessages.LOGIN_SUCCESS, accessToken, refreshTokenObj.getToken(), jwtService.getAccessTokenExpiration() / 1000));
    }

    @PostMapping(path = "/refresh", produces = Endpoints.PRODUCES)
    public ResponseEntity<?> refresh(@RequestBody RefreshTokenRequestDto requestDto) {
        String requestToken = requestDto.getRefreshToken();

        return refreshTokenRepository.findByToken(requestToken)
                .map(token -> {
                    if (token.getExpiresAt().compareTo(Instant.now()) < 0) {
                        refreshTokenRepository.delete(token);
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token was expired. Please make a new signin request");
                    }
                    if (token.isRevoked()) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token has been revoked.");
                    }
                    
                    // Revoke old token
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);

                    // Generate new access and refresh token
                    User user = token.getUser();
                    String accessToken = jwtService.generateAccessToken(user.getName(), user.getId().toString(), user.getRoles());
                    RefreshToken newRefreshToken = createRefreshToken(user);
                    
                    return ResponseEntity.ok(new LoginResponseDto("Token refreshed successfully", accessToken, newRefreshToken.getToken(), jwtService.getAccessTokenExpiration() / 1000));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token is not in database!"));
    }

    @PostMapping(path = "/logout", produces = Endpoints.PRODUCES)
    public ResponseEntity<?> logout(@RequestBody LogoutRequestDto requestDto) {
        String requestToken = requestDto.getRefreshToken();
        refreshTokenRepository.findByToken(requestToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            log.info("Refresh token revoked for user {}", token.getUser().getId());
        });
        return ResponseEntity.ok().body("Log out successful");
    }
}
