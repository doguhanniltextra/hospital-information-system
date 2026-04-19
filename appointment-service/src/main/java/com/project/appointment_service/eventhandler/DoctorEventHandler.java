package com.project.appointment_service.eventhandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.appointment_service.constants.KafkaEndpoints;
import com.project.appointment_service.dto.event.DoctorUpdatedEventDto;
import com.project.appointment_service.model.AppointmentSummary;
import com.project.appointment_service.repository.AppointmentSummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class DoctorEventHandler {

    private static final Logger log = LoggerFactory.getLogger(DoctorEventHandler.class);
    private final AppointmentSummaryRepository appointmentSummaryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DoctorEventHandler(AppointmentSummaryRepository appointmentSummaryRepository) {
        this.appointmentSummaryRepository = appointmentSummaryRepository;
    }

    @KafkaListener(topics = KafkaEndpoints.KAFKA_DOCTOR_UPDATED, groupId = "appointment-service")
    public void handleDoctorUpdate(String payload) {
        try {
            DoctorUpdatedEventDto event = objectMapper.readValue(payload, DoctorUpdatedEventDto.class);
            UUID doctorId = UUID.fromString(event.getDoctorId());
            List<AppointmentSummary> summaries = appointmentSummaryRepository.findByDoctorId(doctorId);

            for (AppointmentSummary summary : summaries) {
                summary.setDoctorName(event.getName());
                summary.setDoctorSpecialization(event.getSpecialization());
                appointmentSummaryRepository.save(summary);
            }
            log.info("Updated {} appointment summaries for doctor {}", summaries.size(), doctorId);
        } catch (Exception ex) {
            log.error("Failed to process doctor update event: {}", ex.getMessage(), ex);
        }
    }
}
