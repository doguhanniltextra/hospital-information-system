package com.project.patient_service.helper;

import com.project.patient_service.dto.InsuranceInfoDto;
import com.project.patient_service.dto.request.CreatePatientServiceRequestDto;
import com.project.patient_service.dto.request.KafkaPatientRequestDto;
import com.project.patient_service.dto.request.UpdatePatientServiceRequestDto;
import com.project.patient_service.exception.EmailAlreadyExistsException;
import com.project.patient_service.exception.PatientNotFoundException;
import com.project.patient_service.model.InsuranceInfo;
import com.project.patient_service.model.InsuranceProviderType;
import com.project.patient_service.model.Patient;
import com.project.patient_service.query.PatientQueryService;
import com.project.patient_service.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserValidator {

    private static final Logger log = LoggerFactory.getLogger(UserValidator.class);

    public void CheckEmailIsExistsOrNotForCreatePatient(CreatePatientServiceRequestDto patientRequestDTO, PatientRepository patientRepository) {
        if( patientRepository.existsByEmail(patientRequestDTO.getEmail())){
            log.error("PATIENT: A patient with this email already exists");
            throw new EmailAlreadyExistsException("A patient with this email already exists.");
        }
    }
    public Patient getPatientForCreatePatient(CreatePatientServiceRequestDto patientRequestDTO) {
        Patient patient = new Patient();
        patient.setName(patientRequestDTO.getName());
        patient.setAddress(patientRequestDTO.getAddress());
        patient.setEmail(patientRequestDTO.getEmail());
        patient.setPhoneNumber(patientRequestDTO.getPhoneNumber());
        patient.setDateOfBirth(parseDateForCreatePatient(patientRequestDTO.getDateOfBirth()));
        patient.setRegisteredDate(parseDateForCreatePatient(patientRequestDTO.getRegisteredDate()));
        patient.setInsuranceInfo(toInsuranceInfo(patientRequestDTO.getInsuranceInfo()));
        patient.setAuthUserId(patientRequestDTO.getAuthUserId());
        return patient;
    }
    private LocalDate parseDateForCreatePatient(String dateStr) {
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + dateStr);
        }
    }
    public void checkEmailIsExistsOrNotForUpdatePatient(UUID id, UpdatePatientServiceRequestDto updatePatientServiceRequestDto, PatientRepository patientRepository) {
        if( patientRepository.existsByEmailAndIdNot(updatePatientServiceRequestDto.getEmail(), id)){
            throw new EmailAlreadyExistsException("A patient with this email already exists.");
        }
    }
    public Patient getPatientForUpdateMethod(UUID id, PatientRepository patientRepository) {
        Patient patient = patientRepository
                .findById(id)
                .orElseThrow(()-> new PatientNotFoundException("Patient Not Found With This ID"));
        return patient;
    }
    public  ResponseEntity<Patient> getPatientResponseEntity(Optional<Patient> currentId) {
        if (currentId.isPresent()) {
            return ResponseEntity.ok(currentId.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    public  ResponseEntity<Boolean> getBooleanResponseEntity(boolean patientByEmail) {
        if(patientByEmail) {
            return ResponseEntity.ok().body(true);
        }
        else {
            return ResponseEntity.notFound().build();
        }
    }
    public boolean isPatientByEmail(String email, PatientQueryService patientQueryService) {
        boolean patientByEmail = patientQueryService.findPatientByEmail(email);
        return patientByEmail;
    }
    public  Map<String, Object> getStringObjectMap(KafkaPatientRequestDto kafkaPatientRequestDto) {
        Map<String, Object> event = new HashMap<>();
        event.put("patientId", kafkaPatientRequestDto.getId().toString());
        event.put("name", kafkaPatientRequestDto.getName());
        event.put("email", kafkaPatientRequestDto.getEmail());
        event.put("eventType", "PATIENT_CREATED");
        return event;
    }
    public void sendKafkaEvent(String json, KafkaTemplate kafkaTemplate) {
        kafkaTemplate.send("patient", json);
    }

    private InsuranceInfo toInsuranceInfo(InsuranceInfoDto insuranceInfoDto) {
        if (insuranceInfoDto == null) {
            return null;
        }
        InsuranceInfo insuranceInfo = new InsuranceInfo();
        insuranceInfo.setProviderName(insuranceInfoDto.getProviderName());
        insuranceInfo.setPolicyNumber(insuranceInfoDto.getPolicyNumber());
        if (insuranceInfoDto.getProviderType() != null && !insuranceInfoDto.getProviderType().isBlank()) {
            insuranceInfo.setProviderType(InsuranceProviderType.valueOf(insuranceInfoDto.getProviderType().toUpperCase()));
        }
        return insuranceInfo;
    }
}
