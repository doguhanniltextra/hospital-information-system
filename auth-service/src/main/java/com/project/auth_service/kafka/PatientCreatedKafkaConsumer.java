package com.project.auth_service.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.auth_service.service.UserProvisioningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens for patient creation events.
 * Triggers the auto-provisioning of user accounts for new clinical patients.
 */
@Component
public class PatientCreatedKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(PatientCreatedKafkaConsumer.class);

    private final UserProvisioningService userProvisioningService;
    private final ObjectMapper objectMapper;

    /**
     * Initializes the consumer with required services.
     * 
     * @param userProvisioningService Service for provisioning users
     * @param objectMapper Mapper for JSON deserialization
     */
    public PatientCreatedKafkaConsumer(UserProvisioningService userProvisioningService,
                                       ObjectMapper objectMapper) {
        this.userProvisioningService = userProvisioningService;
        this.objectMapper = objectMapper;
    }

    /**
     * Listens to the patient-created.v1 topic.
     * Parses the event and initiates user provisioning.
     * 
     * @param message The raw JSON string message from Kafka
     */
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

    /**
     * Extracts a required field from a JSON node.
     * 
     * @param node The JSON node to extract from
     * @param field The name of the field to extract
     * @return The field value as a string
     * @throws IllegalArgumentException if the field is missing or null
     */
    private String getRequiredField(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("Missing required Kafka field: " + field);
        }
        return value.asText();
    }
}
