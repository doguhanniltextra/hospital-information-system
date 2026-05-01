package com.project.notification_service.kafka;

import com.project.notification_service.dto.AppointmentScheduledEvent;
import com.project.notification_service.dto.LabResultCompletedEvent;
import com.project.notification_service.dto.InventoryAlertEvent;
import com.project.notification_service.dto.PatientContactInfo;
import com.project.notification_service.dto.PatientDischargedEvent;
import com.project.notification_service.dto.UserProvisionedEvent;
import com.project.notification_service.grpc.PatientGrpcClient;
import com.project.notification_service.model.NotificationProcessedEvent;
import com.project.notification_service.repository.NotificationProcessedEventRepository;
import com.project.notification_service.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka consumer service responsible for listening to system-wide events and triggering notifications.
 * It handles events from lab-results, appointments, inventory, and authentication services.
 * Implements idempotency checks using NotificationProcessedEventRepository.
 */
@Service
public class KafkaConsumer {
    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);
    private final NotificationService notificationService;
    private final PatientGrpcClient patientGrpcClient;
    private final NotificationProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;
    private final String opsAlertEmails;
    private static final UUID SYSTEM_PATIENT_ID = new UUID(0, 0);

    /**
     * Initializes the Kafka consumer with required services and configurations.
     * 
     * @param notificationService Service for processing and sending notifications
     * @param patientGrpcClient gRPC client for patient data enrichment
     * @param processedEventRepository Repository for idempotency tracking
     * @param objectMapper Mapper for JSON deserialization
     * @param opsAlertEmails Configured email addresses for operational alerts
     */
    public KafkaConsumer(NotificationService notificationService,
                         PatientGrpcClient patientGrpcClient,
                         NotificationProcessedEventRepository processedEventRepository,
                         ObjectMapper objectMapper,
                         @Value("${ops.alert.emails:admin@hospital.com}") String opsAlertEmails) {
        this.notificationService = notificationService;
        this.patientGrpcClient = patientGrpcClient;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
        this.opsAlertEmails = opsAlertEmails;
    }

    /**
     * Consumes lab result completion events and notifies the patient.
     * Data is enriched via gRPC to fetch the patient's latest contact details.
     * 
     * @param event The lab result completion event data
     */
    @Transactional
    @KafkaListener(topics = "lab-result-completed.v1", groupId = "notification-group")
    public void consumeLabResult(LabResultCompletedEvent event) {
        if (!shouldProcess(event.eventId)) return;
        
        log.info("Consumed LabResultCompletedEvent for patient: {}", event.patientId);
        
        // Enrichment via L1 Cache / L2 gRPC
        PatientContactInfo contact = patientGrpcClient.getPatientContactInfo(event.patientId);
        String recipient = (contact != null) ? contact.email() : "patient-" + event.patientId + "@example.com";

        Map<String, Object> variables = new HashMap<>();
        variables.put("patientId", event.patientId);
        variables.put("reportUrl", event.reportPdfUrl);
        
        notificationService.processNotification(event.patientId, recipient, "LAB_RESULT_READY", variables);
        markAsProcessed(event.eventId);
    }

    /**
     * Consumes appointment scheduling events and sends a confirmation to the patient.
     * 
     * @param event The appointment scheduling event data
     */
    @Transactional
    @KafkaListener(topics = "appointment-scheduled.v1", groupId = "notification-group")
    public void consumeAppointment(AppointmentScheduledEvent event) {
        // Note: AppointmentKafkaResponseDto currently doesn't have an eventId, using a composite for now
        String messageId = "APP-" + event.patientId + "-" + event.appointmentDate;
        if (!shouldProcess(messageId)) return;
        
        log.info("Consumed AppointmentScheduledEvent for patient: {}", event.patientId);
        
        // Enrichment via L1 Cache / L2 gRPC
        PatientContactInfo contact = patientGrpcClient.getPatientContactInfo(event.patientId);
        String recipient = (contact != null) ? contact.email() : "patient-" + event.patientId + "@example.com";

        Map<String, Object> variables = new HashMap<>();
        variables.put("appointmentDate", event.appointmentDate);
        variables.put("doctorId", event.doctorId);
        
        notificationService.processNotification(event.patientId, recipient, "APPOINTMENT_CONFIRMATION", variables);
        markAsProcessed(messageId);
    }

    /**
     * Consumes patient discharge events and sends a follow-up notification.
     * 
     * @param event The patient discharge event data
     */
    @Transactional
    @KafkaListener(topics = "patient-discharged.v1", groupId = "notification-group")
    public void consumeDischarge(PatientDischargedEvent event) {
        String messageId = "DIS-" + event.getPatientId() + "-" + event.getAdmissionId();
        if (!shouldProcess(messageId)) return;
        
        log.info("Consumed PatientDischargedEvent for patient: {}", event.getPatientId());
        
        // Enrichment via L1 Cache / L2 gRPC
        PatientContactInfo contact = patientGrpcClient.getPatientContactInfo(event.getPatientId());
        String recipient = (contact != null) ? contact.email() : "patient-" + event.getPatientId() + "@example.com";

        Map<String, Object> variables = new HashMap<>();
        variables.put("patientId", event.getPatientId());
        variables.put("admissionId", event.getAdmissionId());
        
        notificationService.processNotification(event.getPatientId(), recipient, "HOSPITAL_DISCHARGE", variables);
        markAsProcessed(messageId);
    }

    /**
     * Consumes low stock alerts from the inventory service and notifies operations.
     * 
     * @param event The low stock alert event data
     */
    @KafkaListener(topics = "inventory-low-stock.v1", groupId = "notification-group")
    public void consumeLowStockAlert(InventoryAlertEvent event) {
        log.warn("Consumed LowStockAlert for item: {}", event.itemId);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("itemId", event.itemId);
        variables.put("location", event.location);
        variables.put("currentQuantity", event.currentQuantity);
        variables.put("threshold", event.threshold);
        
        // Broadcast to ops alert emails
        String[] recipients = opsAlertEmails.split(",");
        for (String rep : recipients) {
            notificationService.processNotification(SYSTEM_PATIENT_ID, rep.trim(), "LOW_STOCK_ALERT", variables);
        }
    }

    /**
     * Consumes item expiration alerts from the inventory service and notifies operations.
     * 
     * @param event The item expiration alert event data
     */
    @KafkaListener(topics = "inventory-item-expired.v1", groupId = "notification-group")
    public void consumeInventoryExpiryAlert(InventoryAlertEvent event) {
        log.warn("Consumed InventoryExpiryAlert for item: {}", event.itemId);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("itemId", event.itemId);
        variables.put("location", event.location);
        variables.put("expiredQuantity", event.currentQuantity);
        
        // Broadcast to ops alert emails
        String[] recipients = opsAlertEmails.split(",");
        for (String rep : recipients) {
            notificationService.processNotification(SYSTEM_PATIENT_ID, rep.trim(), "INVENTORY_EXPIRY_ALERT", variables);
        }
    }

    /**
     * Checks if a message has already been processed to ensure idempotency.
     * 
     * @param messageId The unique identifier for the message/event
     * @return True if the message should be processed
     */
    private boolean shouldProcess(String messageId) {
        if (messageId == null) return true;
        if (processedEventRepository.existsByMessageId(messageId)) {
            log.warn("Skipping already processed message: {}", messageId);
            return false;
        }
        return true;
    }

    /**
     * Marks a message as processed in the idempotency repository.
     * 
     * @param messageId The unique identifier for the message/event
     */
    private void markAsProcessed(String messageId) {
        if (messageId != null) {
            processedEventRepository.save(new NotificationProcessedEvent(messageId));
        }
    }

    /**
     * Listens to user-provisioned.v1 published by auth-service (raw JSON string).
     * Sends the PATIENT_WELCOME email containing the secure password-set link.
     * Uses stringContainerFactory to avoid JsonDeserializer type mismatch.
     * 
     * @param message The raw JSON string of the UserProvisionedEvent
     */
    @Transactional
    @KafkaListener(topics = "user-provisioned.v1", groupId = "notification-group", containerFactory = "stringContainerFactory")
    public void consumeUserProvisioned(String message) {
        try {
            UserProvisionedEvent event = objectMapper.readValue(message, UserProvisionedEvent.class);

            String messageId = "PROV-" + event.getEventId();
            if (!shouldProcess(messageId)) return;

            log.info("Consumed user-provisioned.v1 for patientId={}, email={}", event.getPatientId(), event.getEmail());

            String resetLink = "https://hospital.com/set-password?token=" + event.getResetToken();

            Map<String, Object> variables = new HashMap<>();
            variables.put("name", event.getName());
            variables.put("email", event.getEmail());
            variables.put("resetLink", resetLink);
            variables.put("resetToken", event.getResetToken());

            // SYSTEM_PATIENT_ID is used since this is a provisioning notification, not a clinical event
            notificationService.processNotification(
                    SYSTEM_PATIENT_ID,
                    event.getEmail(),
                    "PATIENT_WELCOME",
                    variables
            );

            markAsProcessed(messageId);
        } catch (Exception e) {
            log.error("consumeUserProvisioned: Failed to process message: {}", message, e);
        }
    }
}

