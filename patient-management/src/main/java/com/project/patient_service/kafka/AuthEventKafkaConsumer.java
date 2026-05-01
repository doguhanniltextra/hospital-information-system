package com.project.patient_service.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.project.patient_service.command.PatientCommandService;

@Component
public class AuthEventKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuthEventKafkaConsumer.class);

    private final PatientCommandService patientCommandService;
    private final ObjectMapper objectMapper;

    public AuthEventKafkaConsumer(PatientCommandService patientCommandService,
                                  ObjectMapper objectMapper) {
        this.patientCommandService = patientCommandService;
        this.objectMapper = objectMapper;
    }

    /**
     * Listens to the user-provisioned.v1 event published by auth-service after creating a PATIENT account.
     * Updates the patient record with the authUserId to link the two services.
     */
    @KafkaListener(
            topics = "user-provisioned.v1",
            groupId = "${kafka.groups.patient-auth-link}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onUserProvisioned(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);

            String patientId = getField(node, "patientId");
            String authUserId = getField(node, "authUserId");

            if (patientId == null || authUserId == null) {
                log.error("AuthEventConsumer: Missing patientId or authUserId in event: {}", message);
                return;
            }

            log.info("AuthEventConsumer: Linking patientId={} → authUserId={}", patientId, authUserId);
            patientCommandService.linkAuthUser(patientId, authUserId);

        } catch (Exception e) {
            log.error("AuthEventConsumer: Failed to process user-provisioned.v1 message: {}", message, e);
        }
    }

    private String getField(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value == null || value.isNull()) ? null : value.asText();
    }
}
