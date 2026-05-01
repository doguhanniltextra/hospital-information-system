package com.project.auth_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.auth_service.entity.PasswordResetToken;
import com.project.auth_service.entity.Role;
import com.project.auth_service.entity.User;
import com.project.auth_service.repository.PasswordResetTokenRepository;
import com.project.auth_service.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Service for auto-provisioning user accounts for clinical patients.
 * Handles the creation of authentication credentials and propagation of provisioning events.
 */
@Service
public class UserProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(UserProvisioningService.class);
    private static final String USER_PROVISIONED_TOPIC = "user-provisioned.v1";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Initializes the user provisioning service with required dependencies.
     * 
     * @param userRepository Repository for user data
     * @param resetTokenRepository Repository for password reset tokens
     * @param passwordEncoder Encoder for securing temporary passwords
     * @param kafkaTemplate Kafka template for event publication
     * @param objectMapper Mapper for JSON serialization
     */
    public UserProvisioningService(UserRepository userRepository,
                                   PasswordResetTokenRepository resetTokenRepository,
                                   PasswordEncoder passwordEncoder,
                                   KafkaTemplate<String, String> kafkaTemplate,
                                   ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates a PATIENT-role user account for a newly registered clinical patient,
     * generates a 24-hour password reset token, and publishes the user-provisioned.v1 event
     * so that patient-service can update authUserId and notification-service can send the welcome email.
     *
     * @param patientId clinical patient UUID from patient-service
     * @param name      patient's display name
     * @param email     patient's email (used as the login username for uniqueness)
     */
    @Transactional
    public void provisionPatientUser(String patientId, String name, String email) {
        // Idempotency: skip if the user already exists for this email
        if (userRepository.existsByEmail(email)) {
            log.warn("UserProvisioning: User with email={} already exists. Skipping patientId={}", email, patientId);
            return;
        }

        // Create user — email is used as the login username to guarantee uniqueness
        User user = new User();
        user.setName(email); // login username = email for auto-provisioned accounts
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // locked random password
        user.setRoles(Set.of(Role.PATIENT));
        user.setRegisterDate(LocalDate.now());
        user = userRepository.save(user);
        log.info("UserProvisioning: Created PATIENT user id={} for patientId={}", user.getId(), patientId);

        // Generate 24-hour password reset token
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setExpiresAt(Instant.now().plusSeconds(86_400)); // 24 hours
        resetTokenRepository.save(resetToken);

        // Publish user-provisioned.v1 event (no raw password — only reset token)
        try {
            Map<String, String> event = new HashMap<>();
            event.put("eventId", UUID.randomUUID().toString());
            event.put("patientId", patientId);
            event.put("authUserId", user.getId().toString());
            event.put("email", email);
            event.put("name", name);
            event.put("resetToken", resetToken.getToken());

            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(USER_PROVISIONED_TOPIC, json);
            log.info("UserProvisioning: Published {} for patientId={}, authUserId={}", USER_PROVISIONED_TOPIC, patientId, user.getId());
        } catch (Exception e) {
            log.error("UserProvisioning: Failed to publish {} for patientId={}", USER_PROVISIONED_TOPIC, patientId, e);
            throw new RuntimeException("Failed to publish provisioning event — rolling back user creation", e);
        }
    }
}
