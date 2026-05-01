package com.project.patient_service.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.patient_service.constants.LogMessages;
import com.project.patient_service.dto.request.CreatePatientServiceRequestDto;
import com.project.patient_service.dto.request.UpdatePatientServiceRequestDto;
import com.project.patient_service.dto.response.CreatePatientServiceResponseDto;
import com.project.patient_service.dto.response.UpdatePatientServiceResponseDto;
import com.project.patient_service.exception.EmailAlreadyExistsException;
import com.project.patient_service.helper.UserMapper;
import com.project.patient_service.helper.UserValidator;
import com.project.patient_service.model.Patient;
import com.project.patient_service.model.PatientOutboxEvent;
import com.project.patient_service.repository.PatientOutboxRepository;
import com.project.patient_service.repository.PatientRepository;
import com.project.patient_service.utils.SanitizationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.patient_service.event.PatientCreatedEvent;
import com.project.patient_service.event.PatientUpdatedEvent;
import com.project.patient_service.event.PatientDeletedEvent;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class PatientCommandService {

    private final PatientRepository patientRepository;
    private final PatientOutboxRepository patientOutboxRepository;
    private final UserMapper userMapper;
    private final UserValidator userValidator;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(PatientCommandService.class);

    public PatientCommandService(PatientRepository patientRepository,
            PatientOutboxRepository patientOutboxRepository,
            UserMapper userMapper,
            UserValidator userValidator,
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper) {
        this.patientRepository = patientRepository;
        this.patientOutboxRepository = patientOutboxRepository;
        this.userMapper = userMapper;
        this.userValidator = userValidator;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    public CreatePatientServiceResponseDto createPatient(CreatePatientCommand command)
            throws EmailAlreadyExistsException {
        CreatePatientServiceRequestDto patientRequestDTO = command.patientRequest();

        // Sanitize sensitive input fields
        patientRequestDTO.setName(SanitizationUtils.sanitize(patientRequestDTO.getName()));
        patientRequestDTO.setAddress(SanitizationUtils.sanitize(patientRequestDTO.getAddress()));

        userValidator.CheckEmailIsExistsOrNotForCreatePatient(patientRequestDTO, patientRepository);

        Patient patient = userValidator.getPatientForCreatePatient(patientRequestDTO);

        Patient newPatient = patientRepository.save(patient);
        log.info(LogMessages.SERVICE_CREATE_TRIGGERED, newPatient.getId());

        // Transactional Outbox instead of Async Kafka
        try {
            String payload = objectMapper.writeValueAsString(userMapper.getKafkaPatientRequestDto(newPatient));
            patientOutboxRepository.save(new PatientOutboxEvent(
                    newPatient.getId().toString(), "PATIENT", "PATIENT_CREATED", payload));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize patient created event", e);
            throw new RuntimeException("Outbox serialization failure", e);
        }

        // Publish internal event for Read Model synchronization
        eventPublisher.publishEvent(new PatientCreatedEvent(newPatient));

        return userMapper.getCreatePatientServiceResponseDto(patient);
    }

    public UpdatePatientServiceResponseDto updatePatient(UpdatePatientCommand command) {
        UUID id = command.id();
        UpdatePatientServiceRequestDto updateRequest = command.updateRequest();

        // Sanitize sensitive input fields
        updateRequest.setName(SanitizationUtils.sanitize(updateRequest.getName()));
        updateRequest.setAddress(SanitizationUtils.sanitize(updateRequest.getAddress()));

        Patient patient = userValidator.getPatientForUpdateMethod(id, patientRepository);

        userValidator.checkEmailIsExistsOrNotForUpdatePatient(id, updateRequest, patientRepository);
        log.info(LogMessages.SERVICE_UPDATE_TRIGGERED);

        userMapper.getUpdatePatientRequestDto(updateRequest, patient);

        Patient updatedPatient = patientRepository.save(patient);

        // Transactional Outbox for Update
        try {
            String payload = objectMapper.writeValueAsString(userMapper.getKafkaPatientRequestDto(updatedPatient));
            patientOutboxRepository.save(new PatientOutboxEvent(
                    updatedPatient.getId().toString(), "PATIENT", "PATIENT_UPDATED", payload));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize patient updated event", e);
        }

        // Publish internal event for Read Model synchronization
        eventPublisher.publishEvent(new PatientUpdatedEvent(updatedPatient));

        return userMapper.getUpdatePatientServiceResponseDto(updatedPatient);
    }

    public void deletePatient(DeletePatientCommand command) {
        UUID id = command.id();
        log.info(LogMessages.SERVICE_DELETE_TRIGGERED);

        // Transactional Outbox for Delete
        try {
            Map<String, String> deletePayload = Map.of("patientId", id.toString(), "eventType", "PATIENT_DELETED");
            String payload = objectMapper.writeValueAsString(deletePayload);
            patientOutboxRepository.save(new PatientOutboxEvent(
                    id.toString(), "PATIENT", "PATIENT_DELETED", payload));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize patient deleted event", e);
        }

        patientRepository.deleteById(id);

        // Publish internal event for Read Model synchronization
        eventPublisher.publishEvent(new PatientDeletedEvent(id));
    }

    /**
     * Links a clinical patient record to its auth-service account.
     * Called by AuthEventKafkaConsumer when a user-provisioned.v1 event is received.
     * Idempotent: skips if authUserId is already set.
     */
    public void linkAuthUser(String patientId, String authUserId) {
        UUID id;
        try {
            id = UUID.fromString(patientId);
        } catch (IllegalArgumentException e) {
            log.error("linkAuthUser: Invalid patientId format: {}", patientId);
            return;
        }

        Patient patient = patientRepository.findById(id).orElse(null);
        if (patient == null) {
            log.warn("linkAuthUser: Patient not found for id={}", patientId);
            return;
        }
        if (patient.getAuthUserId() != null) {
            log.warn("linkAuthUser: authUserId already set for patientId={}. Skipping.", patientId);
            return;
        }

        patient.setAuthUserId(UUID.fromString(authUserId));
        patientRepository.save(patient);
        log.info("linkAuthUser: Successfully linked patientId={} → authUserId={}", patientId, authUserId);
    }
}

