package com.project.auth_service.helper;

import com.project.auth_service.constants.LogMessages;
import com.project.auth_service.dto.LoginRequestDto;
import com.project.auth_service.dto.RegisterRequestDto;
import com.project.auth_service.entity.Role;
import com.project.auth_service.entity.User;
import com.project.auth_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthValidatorTest {

    private AuthValidator authValidator;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authValidator = new AuthValidator();
    }

    @Test
    void checkIfUsernameAlreadyExists_ShouldReturnBadRequest_WhenExists() {
        RegisterRequestDto dto = new RegisterRequestDto();
        dto.setName("existingUser");
        when(userRepository.existsByName("existingUser")).thenReturn(true);

        ResponseEntity<String> response = authValidator.checkIfUsernameAlreadyExistsOrNotForRegisterMethod(dto,
                userRepository);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(LogMessages.USERNAME_ALREADY_EXISTS, response.getBody());
    }

    @Test
    void checkIfUsernameAlreadyExists_ShouldReturnNull_WhenDoesNotExist() {
        RegisterRequestDto dto = new RegisterRequestDto();
        dto.setName("newUser");
        when(userRepository.existsByName("newUser")).thenReturn(false);

        ResponseEntity<String> response = authValidator.checkIfUsernameAlreadyExistsOrNotForRegisterMethod(dto,
                userRepository);

        assertNull(response);
    }

    @Test
    void registerRequestDtoToUser_ShouldMapCorrectly() {
        RegisterRequestDto dto = new RegisterRequestDto();
        dto.setName("testuser");
        dto.setPassword("pass123");
        dto.setEmail("test@example.com");
        // dto.setRoles(java.util.Arrays.asList(Role.DOCTOR)); // Removed as
        // RegisterRequestDto no longer has roles

        when(passwordEncoder.encode("pass123")).thenReturn("encodedPass");

        User user = authValidator.registerRequestDtoToUserForRegisterMethod(dto, passwordEncoder);

        assertEquals("testuser", user.getName());
        assertEquals("encodedPass", user.getPassword());
        assertEquals("test@example.com", user.getEmail());
        assertTrue(user.getRoles().contains(Role.PATIENT)); // Should now default to PATIENT
    }

    @Test
    void checkIfPasswordIsInvalidForLogin_ShouldReturnUnauthorized_WhenMismatch() {
        LoginRequestDto dto = new LoginRequestDto();
        dto.setPassword("wrongPass");
        User user = new User();
        user.setPassword("correctPass");

        when(passwordEncoder.matches("wrongPass", "correctPass")).thenReturn(false);

        ResponseEntity<String> response = authValidator.checkIfPasswordIsInvalidForLogin(dto, user, passwordEncoder);

        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(LogMessages.CHECK_IF_USERNAME_OR_PASSWORD_IS_INVALID, response.getBody());
    }

    @Test
    void checkIfUsernameOrPasswordIsEmptyForLoginMethod_ShouldReturnUnauthorized_WhenUserIsNull() {
        ResponseEntity<String> response = authValidator.checkIfUsernameOrPasswordIsEmptyForLoginMethod(null);

        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(LogMessages.CHECK_IF_USERNAME_OR_PASSWORD_IS_EMPTY, response.getBody());
    }
}
