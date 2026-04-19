package com.project.appointment_service.eventhandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.appointment_service.constants.KafkaEndpoints;
import com.project.appointment_service.dto.event.PatientUpdatedEventDto;
import com.project.appointment_service.model.AppointmentSummary;
import com.project.appointment_service.repository.AppointmentSummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class PatientEventHandler {

    private static final Logger log = LoggerFactory.getLogger(PatientEventHandler.class);
    private final AppointmentSummaryRepository appointmentSummaryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PatientEventHandler(AppointmentSummaryRepository appointmentSummaryRepository) {
        this.appointmentSummaryRepository = appointmentSummaryRepository;
    }

    @KafkaListener(topics = KafkaEndpoints.KAFKA_PATIENT_UPDATED, groupId = "appointment-service")
    public void handlePatientUpdate(String payload) {
        try {
            PatientUpdatedEventDto event = objectMapper.readValue(payload, PatientUpdatedEventDto.class);
            UUID patientId = UUID.fromString(event.getPatientId());
            List<AppointmentSummary> summaries = appointmentSummaryRepository.findByPatientId(patientId);

            for (AppointmentSummary summary : summaries) {
                summary.setPatientName(event.getName());
                summary.setPatientEmail(event.getEmail());
                appointmentSummaryRepository.save(summary);
            }
            log.info("Updated {} appointment summaries for patient {}", summaries.size(), patientId);
        } catch (Exception ex) {
            log.error("Failed to process patient update event: {}", ex.getMessage(), ex);
        }
    }
}
