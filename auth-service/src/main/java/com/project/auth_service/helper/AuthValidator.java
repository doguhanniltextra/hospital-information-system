package com.project.auth_service.helper;

import com.project.auth_service.constants.LogMessages;
import com.project.auth_service.dto.LoginRequestDto;
import com.project.auth_service.dto.RegisterRequestDto;
import com.project.auth_service.entity.User;
import com.project.auth_service.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Helper component for validating authentication and registration requests.
 * Provides methods for checking duplicate users, password verification, and DTO-to-Entity mapping.
 */
@Component
public class AuthValidator {

    /**
     * Checks if a username already exists in the system during registration.
     * 
     * @param registerRequestDto Registration data containing the name to check
     * @param userRepository Repository for user lookups
     * @return ResponseEntity with error if exists, or null if unique
     */
    public ResponseEntity<String> checkIfUsernameAlreadyExistsOrNotForRegisterMethod(
            RegisterRequestDto registerRequestDto, UserRepository userRepository) {
        if (userRepository.existsByName(registerRequestDto.getName())) {
            return ResponseEntity.badRequest().body(LogMessages.USERNAME_ALREADY_EXISTS);
        }
        return null;
    }

    /**
     * Maps a standard registration DTO to a User entity.
     * Assigns the default PATIENT role.
     * 
     * @param registerRequestDto Source DTO
     * @param passwordEncoder Encoder for the user's password
     * @return User entity populated with DTO data
     */
    public User registerRequestDtoToUserForRegisterMethod(RegisterRequestDto registerRequestDto,
            PasswordEncoder passwordEncoder) {
        User user = new User();
        user.setName(registerRequestDto.getName());
        user.setPassword(passwordEncoder.encode(registerRequestDto.getPassword()));
        user.setEmail(registerRequestDto.getEmail());
        user.setRegisterDate(registerRequestDto.getRegisterDate() != null ? registerRequestDto.getRegisterDate() : java.time.LocalDate.now());

        user.getRoles().add(com.project.auth_service.entity.Role.PATIENT);
        return user;
    }

    /**
     * Maps an administrative registration DTO to a User entity.
     * Supports multiple roles; defaults to PATIENT if none provided.
     * 
     * @param registerRequestDto Source DTO
     * @param passwordEncoder Encoder for the user's password
     * @return User entity populated with DTO data and roles
     */
    public User adminRegisterRequestDtoToUser(com.project.auth_service.dto.AdminRegisterRequestDto registerRequestDto,
            PasswordEncoder passwordEncoder) {
        User user = new User();
        user.setName(registerRequestDto.getName());
        user.setPassword(passwordEncoder.encode(registerRequestDto.getPassword()));
        user.setEmail(registerRequestDto.getEmail());
        user.setRegisterDate(registerRequestDto.getRegisterDate() != null ? registerRequestDto.getRegisterDate() : java.time.LocalDate.now());

        if (registerRequestDto.getRoles() != null && !registerRequestDto.getRoles().isEmpty()) {
            user.getRoles().addAll(registerRequestDto.getRoles());
        } else {
            user.getRoles().add(com.project.auth_service.entity.Role.PATIENT);
        }
        return user;
    }

    /**
     * Validates if the provided login password matches the stored user password.
     * 
     * @param loginRequestDto Login credentials from request
     * @param user User entity from database
     * @param passwordEncoder Encoder used for verification
     * @return ResponseEntity with error if mismatch, or null if valid
     */
    public ResponseEntity<String> checkIfPasswordIsInvalidForLogin(LoginRequestDto loginRequestDto, User user,
            PasswordEncoder passwordEncoder) {
        if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(LogMessages.CHECK_IF_USERNAME_OR_PASSWORD_IS_INVALID);
        }
        return null;
    }

    /**
     * Validates that the user entity exists for a login attempt.
     * 
     * @param user User entity to check
     * @return ResponseEntity with error if null, or null if exists
     */
    public ResponseEntity<String> checkIfUsernameOrPasswordIsEmptyForLoginMethod(User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(LogMessages.CHECK_IF_USERNAME_OR_PASSWORD_IS_EMPTY);
        }
        return null;
    }

}
