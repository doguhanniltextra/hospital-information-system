package com.project.auth_service.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.auth_service.service.UserProvisioningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PatientCreatedKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(PatientCreatedKafkaConsumer.class);

    private final UserProvisioningService userProvisioningService;
    private final ObjectMapper objectMapper;

    public PatientCreatedKafkaConsumer(UserProvisioningService userProvisioningService,
                                       ObjectMapper objectMapper) {
        this.userProvisioningService = userProvisioningService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "patient-created.v1",
            groupId = "${kafka.groups.auth-patient-provisioning}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPatientCreated(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);

            String patientId = getRequiredField(node, "patientId");
            String name = getRequiredField(node, "name");
            String email = getRequiredField(node, "email");

            log.info("PatientCreatedConsumer: Received PATIENT_CREATED for patientId={}, email={}", patientId, email);
            userProvisioningService.provisionPatientUser(patientId, name, email);

        } catch (Exception e) {
            // Log and let the container commit the offset — dead-letter on repeated failures
            // should be handled by DLQ configuration (future improvement)
            log.error("PatientCreatedConsumer: Failed to process message: {}", message, e);
        }
    }

    private String getRequiredField(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("Missing required Kafka field: " + field);
        }
        return value.asText();
    }
}
