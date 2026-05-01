package com.project.appointment_service.service;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.project.appointment_service.dto.event.AppointmentOutboxPayload;
import com.project.appointment_service.model.AppointmentOutboxEvent;
import com.project.appointment_service.model.AppointmentStatus;
import com.project.appointment_service.repository.AppointmentOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.appointment_service.constants.LogMessages;
import com.project.appointment_service.dto.AppointmentDTO;
import com.project.appointment_service.dto.AppointmentKafkaResponseDto;
import com.project.appointment_service.dto.DoctorAvailabilityResponseDTO;
import com.project.appointment_service.dto.PatientInfoDTO;
import com.project.appointment_service.dto.request.CreateAppointmentServiceRequestDto;
import com.project.appointment_service.dto.response.AppointmentSummaryDto;
import com.project.appointment_service.dto.response.CreateAppointmentServiceResponseDto;
import com.project.appointment_service.exception.CustomNotFoundException;
import com.project.appointment_service.helper.AppointmentMapper;
import com.project.appointment_service.helper.AppointmentSummaryMapper;
import com.project.appointment_service.helper.AppointmentValidator;
import com.project.appointment_service.kafka.KafkaProducer;
import com.project.appointment_service.service.AppointmentSummaryService;
import com.project.appointment_service.utils.IdValidation;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.project.appointment_service.model.Appointment;
import com.project.appointment_service.repository.AppointmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for managing appointment operations.
 * Handles creation, updates, deletions, and payment status tracking for appointments.
 * Incorporates saga orchestration for complex creation flows and outbox pattern for reliable event delivery.
 */
@Service
public class AppointmentService {

    private final AppointmentOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final KafkaProducer kafkaProducer;
    private final AppointmentRepository appointmentRepository;
    private final IdValidation idValidation;
    private final AppointmentSummaryService appointmentSummaryService;
    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final AppointmentMapper appointmentMapper;
    private final AppointmentValidator appointmentValidator;
    private final com.project.appointment_service.saga.CreateAppointmentSaga createAppointmentSaga;

    /**
     * Initializes the AppointmentService with required dependencies.
     * 
     * @param kafkaProducer Producer for Kafka event messaging
     * @param appointmentRepository Repository for appointment data
     * @param idValidation Service for cross-service ID validation via gRPC
     * @param appointmentMapper Mapper for converting between DTOs and entities
     * @param appointmentValidator Validator for appointment business rules
     * @param appointmentSummaryService Service for managing appointment summaries
     * @param createAppointmentSaga Saga orchestrator for creating appointments
     * @param outboxRepository Repository for tracking outbox events
     * @param objectMapper Mapper for JSON serialization
     */
    public AppointmentService(KafkaProducer kafkaProducer, AppointmentRepository appointmentRepository, IdValidation idValidation,
                              AppointmentMapper appointmentMapper, AppointmentValidator appointmentValidator,
                              AppointmentSummaryService appointmentSummaryService,
                              com.project.appointment_service.saga.CreateAppointmentSaga createAppointmentSaga,
                              AppointmentOutboxRepository outboxRepository,
                              ObjectMapper objectMapper) {
        this.kafkaProducer = kafkaProducer;
        this.appointmentRepository = appointmentRepository;
        this.idValidation = idValidation;
        this.appointmentMapper = appointmentMapper;
        this.appointmentValidator = appointmentValidator;
        this.appointmentSummaryService = appointmentSummaryService;
        this.createAppointmentSaga = createAppointmentSaga;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Orchestrates the creation of a new appointment using the Saga pattern.
     * 
     * @param createAppointmentServiceRequestDto The request details for the new appointment
     * @return The response details of the created (or pending) appointment
     */
    public CreateAppointmentServiceResponseDto createAppointment(CreateAppointmentServiceRequestDto createAppointmentServiceRequestDto) {
        log.info("Reflecting CreateAppointment request to Saga Orchestrator");
        return createAppointmentSaga.execute(createAppointmentServiceRequestDto);
    }

    /**
     * Updates an existing appointment's details.
     * 
     * @param appointment The appointment entity containing updated information
     * @return ResponseEntity containing the updated and saved appointment
     */
    public ResponseEntity<Appointment> updateAppointment(Appointment appointment) {
        log.info(LogMessages.SERVICE_UPDATE_STARTING, appointment.getId());
        Appointment existingAppointment = appointmentRepository.findById(appointment.getId())
                .orElseThrow(() -> new CustomNotFoundException("Appointment not found: " + appointment.getId()));
        appointmentMapper.updateAppointmentExtracted(appointment, existingAppointment);
        Appointment saved = appointmentRepository.save(existingAppointment);
        appointmentSummaryService.createOrUpdateSummary(saved);
        log.info(LogMessages.SERVICE_UPDATE_ENDED);
        return ResponseEntity.ok().body(saved);
    }

    /**
     * Deletes an appointment by its unique identifier.
     * Also removes the associated appointment summary.
     * 
     * @param id The UUID of the appointment to delete
     */
    public void deleteAppointment(UUID id) {
        log.info(LogMessages.SERVICE_DELETE_TRIGGERED, id);
        appointmentRepository.deleteById(id);
        appointmentSummaryService.deleteSummary(id);
    }

    /**
     * Updates the payment status of an appointment.
     * Triggers a status change to PAYMENT_CONFIRMED if successful and registers an outbox event.
     * 
     * @param id The UUID of the appointment
     * @param status The new payment status (true for paid, false for failed)
     */
    @Transactional
    public void updatePaymentStatus(UUID id, boolean status) {
        log.info(LogMessages.SERVICE_UPDATE_PAYMENT_STATUS_TRIGGERED);

        Appointment appointment = appointmentValidator.getAppointmentForUpdatePaymentStatus(id, appointmentRepository);

        appointment.setPaymentStatus(status);
        if (status) {
            appointment.setStatus(AppointmentStatus.PAYMENT_CONFIRMED);
        }
        
        appointmentRepository.save(appointment);
        appointmentSummaryService.updatePaymentStatus(id, status);

        // Fetch meta-data for Outbox
        PatientInfoDTO patientInfo = idValidation.fetchPatientInfo(appointment.getPatientId());

        try {
            AppointmentOutboxPayload payloadDto = new AppointmentOutboxPayload();
            payloadDto.setAppointmentId(appointment.getId().toString());
            payloadDto.setPatientId(appointment.getPatientId().toString());
            payloadDto.setDoctorId(appointment.getDoctorId().toString());
            payloadDto.setPatientEmail(patientInfo != null ? patientInfo.getEmail() : "unknown@example.com");
            payloadDto.setAmount(appointment.getAmount());
            payloadDto.setStatus(appointment.getStatus().name());
            payloadDto.setAction(status ? "PAYMENT_CONFIRMED" : "PAYMENT_FAILED");
            payloadDto.setTimestamp(System.currentTimeMillis());

            String jsonPayload = objectMapper.writeValueAsString(payloadDto);

            AppointmentOutboxEvent outboxEvent = new AppointmentOutboxEvent(
                appointment.getId().toString(), "APPOINTMENT", "PAYMENT_UPDATE", jsonPayload);
            outboxRepository.save(outboxEvent);
            
            log.info("Outbox: Registered payment update for appointment {}", id);
        } catch (Exception e) {
            log.error("Outbox: Failed to register payment update", e);
            throw new RuntimeException("Payment update failed due to outbox error", e);
        }
    }

    /**
     * Retrieves a paginated list of all appointments.
     * 
     * @param pageable Pagination and sorting information
     * @return A page of appointment entities
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<Appointment> getAllAppointments(Pageable pageable) {
        log.info(LogMessages.SERVICE_GET_ALL_TRIGGERED);
        return appointmentRepository.findAll(pageable);
    }

    /**
     * Retrieves a paginated list of appointment summaries.
     * Summaries provide a flattened view of appointment, patient, and doctor data.
     * 
     * @param pageable Pagination and sorting information
     * @return A page of appointment summary DTOs
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<AppointmentSummaryDto> getAllAppointmentSummaries(Pageable pageable) {
        log.info("AppointmentService: getAllAppointmentSummaries triggered");
        return appointmentSummaryService.getAllAppointmentSummaries(pageable);
    }
}
