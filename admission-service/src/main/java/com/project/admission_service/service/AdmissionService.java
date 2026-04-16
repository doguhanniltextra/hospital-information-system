package com.project.admission_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.admission_service.dto.AdmissionRequest;
import com.project.admission_service.dto.PatientDischargedEvent;
import com.project.admission_service.dto.PatientResponse;
import com.project.admission_service.model.*;
import com.project.admission_service.repository.*;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class AdmissionService {
    private static final Logger log = LoggerFactory.getLogger(AdmissionService.class);

    private final WardRepository wardRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final AdmissionRepository admissionRepository;
    private final AdmissionOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${patient.service.url}")
    private String patientServiceUrl;

    public AdmissionService(
            WardRepository wardRepository,
            RoomRepository roomRepository,
            BedRepository bedRepository,
            AdmissionRepository admissionRepository,
            AdmissionOutboxRepository outboxRepository,
            ObjectMapper objectMapper,
            RestTemplate restTemplate) {
        this.wardRepository = wardRepository;
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
        this.admissionRepository = admissionRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public Admission admitPatient(AdmissionRequest request) {
        // Logic to find first available bed in specified ward
        List<Room> rooms = roomRepository.findByWardId(request.getWardId());
        Bed selectedBed = null;
        for (Room room : rooms) {
            List<Bed> emptyBeds = bedRepository.findByRoomIdAndStatus(room.getId(), BedStatus.EMPTY);
            if (!emptyBeds.isEmpty()) {
                selectedBed = emptyBeds.get(0);
                break;
            }
        }

        if (selectedBed == null) {
            throw new RuntimeException("No available beds in the selected ward.");
        }

        // Update bed status
        selectedBed.setStatus(BedStatus.OCCUPIED);
        bedRepository.save(selectedBed);

        // Create admission record
        Admission admission = new Admission();
        admission.setPatientId(request.getPatientId());
        admission.setDoctorId(request.getDoctorId());
        admission.setBedId(selectedBed.getId());
        admission.setAdmissionDate(LocalDateTime.now());
        admission.setStatus(AdmissionStatus.ACTIVE);
        Admission savedAdmission = admissionRepository.save(admission);

        // No outbox event for start of admission (billing starts at midnight or based on DailyCharge)
        // But we could add one for Analytics if needed.

        return savedAdmission;
    }

    @Transactional
    public Admission dischargePatient(UUID admissionId) {
        Admission admission = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new RuntimeException("Admission not found."));

        if (admission.getStatus() == AdmissionStatus.DISCHARGED) {
            throw new IllegalStateException("Patient already discharged.");
        }

        // Free the bed
        Bed bed = bedRepository.findById(admission.getBedId()).get();
        bed.setStatus(BedStatus.CLEANING);
        bedRepository.save(bed);

        // Update admission
        admission.setDischargeDate(LocalDateTime.now());
        admission.setStatus(AdmissionStatus.DISCHARGED);
        Admission savedAdmission = admissionRepository.save(admission);

        // Create Outbox Event for Discharge
        try {
            // Asynchronously fetch patient data for event enrichment
            PatientResponse patientResponse = fetchPatientDataAsync(admission.getPatientId());

            PatientDischargedEvent event = new PatientDischargedEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setPatientId(admission.getPatientId());
            event.setDoctorId(admission.getDoctorId());
            event.setPatientEmail(patientResponse != null ? patientResponse.getEmail() : "patient-" + admission.getPatientId() + "@example.com");
            event.setPatientPhone(patientResponse != null && patientResponse.getPhoneNumber() != null ? patientResponse.getPhoneNumber() : "555-0100"); 
            event.setAdmissionId(admission.getId());

            AdmissionOutboxEvent outboxEvent = new AdmissionOutboxEvent();
            outboxEvent.setAggregateType("ADMISSION");
            outboxEvent.setAggregateId(admission.getId().toString());
            outboxEvent.setEventType("PATIENT_DISCHARGED");
            outboxEvent.setPayloadJson(objectMapper.writeValueAsString(event));
            outboxEvent.setStatus("PENDING");
            outboxEvent.setRetryCount(0);
            outboxEvent.setCreatedAt(Instant.now());
            outboxRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing discharge event JSON.", e);
        }

        return savedAdmission;
    }

    public List<Admission> getActiveAdmissions() {
        return admissionRepository.findByStatus(AdmissionStatus.ACTIVE);
    }

    private PatientResponse fetchPatientDataAsync(UUID patientId) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                log.info("Fetching enrichment data for patient: {}", patientId);
                return restTemplate.getForObject(patientServiceUrl + "/" + patientId, PatientResponse.class);
            }).orTimeout(2, TimeUnit.SECONDS).get();
        } catch (Exception e) {
            log.warn("Failed to fetch patient data for enrichment: {}. Proceeding with placeholders.", e.getMessage());
            return null;
        }
    }
}
