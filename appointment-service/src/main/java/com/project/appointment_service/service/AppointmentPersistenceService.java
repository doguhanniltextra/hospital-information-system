package com.project.appointment_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.appointment_service.dto.DoctorInfoDTO;
import com.project.appointment_service.dto.PatientInfoDTO;
import com.project.appointment_service.dto.event.AppointmentOutboxPayload;
import com.project.appointment_service.dto.request.CreateAppointmentServiceRequestDto;
import com.project.appointment_service.helper.AppointmentMapper;
import com.project.appointment_service.model.Appointment;
import com.project.appointment_service.model.AppointmentOutboxEvent;
import com.project.appointment_service.model.AppointmentStatus;
import com.project.appointment_service.repository.AppointmentOutboxRepository;
import com.project.appointment_service.repository.AppointmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for atomic persistence of Appointments and their corresponding Outbox events.
 * Ensures that business data and integration events are written within the same database transaction.
 */
@Service
public class AppointmentPersistenceService {
    private static final Logger log = LoggerFactory.getLogger(AppointmentPersistenceService.class);

    private final AppointmentRepository appointmentRepository;
    private final AppointmentOutboxRepository outboxRepository;
    private final AppointmentMapper appointmentMapper;
    private final AppointmentSummaryService appointmentSummaryService;
    private final ObjectMapper objectMapper;

    /**
     * Initializes the AppointmentPersistenceService with required repositories and mappers.
     * 
     * @param appointmentRepository Repository for Appointment entities
     * @param outboxRepository Repository for Transactional Outbox events
     * @param appointmentMapper Mapper for entity conversions
     * @param appointmentSummaryService Service for managing appointment summaries
     * @param objectMapper JSON mapper for serializing outbox payloads
     */
    public AppointmentPersistenceService(AppointmentRepository appointmentRepository,
                                         AppointmentOutboxRepository outboxRepository,
                                         AppointmentMapper appointmentMapper,
                                         AppointmentSummaryService appointmentSummaryService,
                                         ObjectMapper objectMapper) {
        this.appointmentRepository = appointmentRepository;
        this.outboxRepository = outboxRepository;
        this.appointmentMapper = appointmentMapper;
        this.appointmentSummaryService = appointmentSummaryService;
        this.objectMapper = objectMapper;
    }

    /**
     * Persists an appointment and creates a corresponding outbox event in a single transaction.
     * 1. Maps the request to an Appointment entity.
     * 2. Sets the initial status to PAYMENT_PENDING.
     * 3. Saves the appointment.
     * 4. Updates the appointment summary.
     * 5. Serializes and saves the outbox event for Kafka propagation.
     * 
     * @param request The appointment creation request
     * @param patientInfo Metadata about the patient obtained via gRPC
     * @param doctorInfo Metadata about the doctor obtained via gRPC
     * @return The persisted Appointment entity
     * @throws RuntimeException if serialization fails, triggering a transaction rollback
     */
    @Transactional
    public Appointment persistAppointmentAndOutbox(CreateAppointmentServiceRequestDto request, PatientInfoDTO patientInfo, DoctorInfoDTO doctorInfo) {
        Appointment appointment = appointmentMapper.getAppointment(request);
        
        // Initial Tightened Status
        appointment.setStatus(AppointmentStatus.PAYMENT_PENDING); 
        
        Appointment saved = appointmentRepository.save(appointment);
        
        // Update Summary atomically with provided info
        appointmentSummaryService.createOrUpdateSummary(saved, patientInfo, doctorInfo);

        try {
            // Create Structured Outbox Payload
            AppointmentOutboxPayload payloadDto = new AppointmentOutboxPayload();
            payloadDto.setAppointmentId(saved.getId().toString());
            payloadDto.setPatientId(patientInfo.getId());
            payloadDto.setDoctorId(doctorInfo.getId());
            payloadDto.setPatientEmail(patientInfo.getEmail());
            payloadDto.setAmount(saved.getAmount());
            payloadDto.setStatus(saved.getStatus().name());
            payloadDto.setAction("APPOINTMENT_CREATED");
            payloadDto.setTimestamp(System.currentTimeMillis());

            String jsonPayload = objectMapper.writeValueAsString(payloadDto);

            AppointmentOutboxEvent outboxEvent = new AppointmentOutboxEvent(
                saved.getId().toString(), "APPOINTMENT", "APPOINTMENT_CREATED", jsonPayload);
            outboxRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            log.error("Outbox serialization failure", e);
            throw new RuntimeException("Creation failed due to outbox error", e);
        }
        
        return saved;
    }
}
