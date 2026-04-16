package com.project.notification_service.kafka;

import com.project.notification_service.dto.AppointmentScheduledEvent;
import com.project.notification_service.dto.LabResultCompletedEvent;
import com.project.notification_service.dto.InventoryAlertEvent;
import com.project.notification_service.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class KafkaConsumer {
    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);
    private final NotificationService notificationService;
    private final String opsAlertEmails;
    private static final UUID SYSTEM_PATIENT_ID = new UUID(0, 0);

    public KafkaConsumer(NotificationService notificationService,
                         @Value("${ops.alert.emails:admin@hospital.com}") String opsAlertEmails) {
        this.notificationService = notificationService;
        this.opsAlertEmails = opsAlertEmails;
    }

    @KafkaListener(topics = "lab-result-completed.v1", groupId = "notification-group")
    public void consumeLabResult(LabResultCompletedEvent event) {
        log.info("Consumed LabResultCompletedEvent for patient: {}", event.patientId);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("patientId", event.patientId);
        variables.put("reportUrl", event.reportPdfUrl);
        
        // Use enriched email if present, otherwise fallback to a mock for now
        String recipient = (event.patientEmail != null) ? event.patientEmail : "patient-" + event.patientId + "@example.com";
        
        notificationService.processNotification(event.patientId, recipient, "LAB_RESULT_READY", variables);
    }

    @KafkaListener(topics = "appointment-scheduled.v1", groupId = "notification-group")
    public void consumeAppointment(AppointmentScheduledEvent event) {
        log.info("Consumed AppointmentScheduledEvent for patient: {}", event.patientId);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("appointmentDate", event.appointmentDate);
        variables.put("doctorId", event.doctorId);
        
        String recipient = (event.patientEmail != null) ? event.patientEmail : "patient-" + event.patientId + "@example.com";
        
        notificationService.processNotification(event.patientId, recipient, "APPOINTMENT_CONFIRMATION", variables);
    }

    @KafkaListener(topics = "patient-discharged.v1", groupId = "notification-group")
    public void consumeDischarge(com.project.notification_service.dto.PatientDischargedEvent event) {
        log.info("Consumed PatientDischargedEvent for patient: {}", event.getPatientId());
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("patientId", event.getPatientId());
        variables.put("admissionId", event.getAdmissionId());
        
        String recipient = (event.getPatientEmail() != null) ? event.getPatientEmail() : "patient-" + event.getPatientId() + "@example.com";
        
        notificationService.processNotification(event.getPatientId(), recipient, "HOSPITAL_DISCHARGE", variables);
    }

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

    @KafkaListener(topics = "inventory-item-expired.v1", groupId = "notification-group")
    public void consumeInventoryExpiryAlert(InventoryAlertEvent event) {
        log.warn("Consumed InventoryExpiryAlert for item: {}", event.itemId);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("itemId", event.itemId);
        variables.put("location", event.location);
        variables.put("expiredQuantity", event.currentQuantity); // the quantity that expired is in currentQuantity
        
        // Broadcast to ops alert emails
        String[] recipients = opsAlertEmails.split(",");
        for (String rep : recipients) {
            notificationService.processNotification(SYSTEM_PATIENT_ID, rep.trim(), "INVENTORY_EXPIRY_ALERT", variables);
        }
    }
}
