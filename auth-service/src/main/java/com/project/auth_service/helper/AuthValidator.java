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

@Component
public class AuthValidator {

    public ResponseEntity<String> checkIfUsernameAlreadyExistsOrNotForRegisterMethod(
            RegisterRequestDto registerRequestDto, UserRepository userRepository) {
        if (userRepository.existsByName(registerRequestDto.getName())) {
            return ResponseEntity.badRequest().body(LogMessages.USERNAME_ALREADY_EXISTS);
        }
        return null;
    }

    public User registerRequestDtoToUserForRegisterMethod(RegisterRequestDto registerRequestDto,
            PasswordEncoder passwordEncoder) {
        User user = new User();
        user.setName(registerRequestDto.getName());
        user.setPassword(passwordEncoder.encode(registerRequestDto.getPassword()));
        user.setEmail(registerRequestDto.getEmail());
        user.setRegisterDate(registerRequestDto.getRegisterDate());

        user.getRoles().add(com.project.auth_service.entity.Role.PATIENT);
        return user;
    }

    public User adminRegisterRequestDtoToUser(com.project.auth_service.dto.AdminRegisterRequestDto registerRequestDto,
            PasswordEncoder passwordEncoder) {
        User user = new User();
        user.setName(registerRequestDto.getName());
        user.setPassword(passwordEncoder.encode(registerRequestDto.getPassword()));
        user.setEmail(registerRequestDto.getEmail());
        user.setRegisterDate(registerRequestDto.getRegisterDate());

        if (registerRequestDto.getRoles() != null && !registerRequestDto.getRoles().isEmpty()) {
            user.getRoles().addAll(registerRequestDto.getRoles());
        } else {
            user.getRoles().add(com.project.auth_service.entity.Role.PATIENT);
        }
        return user;
    }

    public ResponseEntity<String> checkIfPasswordIsInvalidForLogin(LoginRequestDto loginRequestDto, User user,
            PasswordEncoder passwordEncoder) {
        if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(LogMessages.CHECK_IF_USERNAME_OR_PASSWORD_IS_INVALID);
        }
        return null;
    }

    public ResponseEntity<String> checkIfUsernameOrPasswordIsEmptyForLoginMethod(User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(LogMessages.CHECK_IF_USERNAME_OR_PASSWORD_IS_EMPTY);
        }
        return null;
    }

}
