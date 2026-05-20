package com.project.admission_service.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.admission_service.dto.AdmissionRequest;
import com.project.admission_service.dto.PatientDischargedEvent;
import com.project.admission_service.event.AdmissionCreatedEvent;
import com.project.admission_service.event.AdmissionUpdatedEvent;
import com.project.admission_service.exception.EntityNotFoundException;
import com.project.admission_service.grpc.DoctorGrpcClient;
import com.project.admission_service.grpc.PatientGrpcClient;
import com.project.admission_service.model.*;
import com.project.admission_service.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AdmissionCommandService {
    private static final Logger log = LoggerFactory.getLogger(AdmissionCommandService.class);

    private final WardRepository wardRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final AdmissionRepository admissionRepository;
    private final AdmissionOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final AdmissionProcessedEventRepository processedEventRepository;
    private final PatientGrpcClient patientGrpcClient;
    private final DoctorGrpcClient doctorGrpcClient;
    private final ApplicationEventPublisher eventPublisher;

    public AdmissionCommandService(
            WardRepository wardRepository,
            RoomRepository roomRepository,
            BedRepository bedRepository,
            AdmissionRepository admissionRepository,
            AdmissionOutboxRepository outboxRepository,
            AdmissionProcessedEventRepository processedEventRepository,
            PatientGrpcClient patientGrpcClient,
            DoctorGrpcClient doctorGrpcClient,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher) {
        this.wardRepository = wardRepository;
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
        this.admissionRepository = admissionRepository;
        this.outboxRepository = outboxRepository;
        this.processedEventRepository = processedEventRepository;
        this.patientGrpcClient = patientGrpcClient;
        this.doctorGrpcClient = doctorGrpcClient;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    public Admission admitPatient(AdmissionRequest request) {
        // Step 1: Validate existence of Patient and Doctor (Integrity over Availability)
        if (!patientGrpcClient.existsById(request.getPatientId())) {
            throw new EntityNotFoundException("Patient with ID " + request.getPatientId() + " does not exist.");
        }
        if (!doctorGrpcClient.existsById(request.getDoctorId())) {
            throw new EntityNotFoundException("Doctor with ID " + request.getDoctorId() + " does not exist.");
        }

        // Step 2: Atomic Bed Selection & Locking
        // We use SKIP LOCKED to handle high concurrency without screen freezes or double-booking.
        Bed selectedBed = bedRepository.findFirstEmptyBedInWardForUpdate(request.getWardId())
                .orElseThrow(() -> new RuntimeException("No available beds in the selected ward."));

        selectedBed.setStatus(BedStatus.OCCUPIED);
        bedRepository.save(selectedBed);

        Admission admission = new Admission();
        admission.setPatientId(request.getPatientId());
        admission.setDoctorId(request.getDoctorId());
        admission.setBedId(selectedBed.getId());
        admission.setAdmissionDate(LocalDateTime.now());
        admission.setStatus(AdmissionStatus.ACTIVE);
        Admission savedAdmission = admissionRepository.save(admission);

        eventPublisher.publishEvent(new AdmissionCreatedEvent(savedAdmission, Instant.now()));

        return savedAdmission;
    }

    public Admission dischargePatient(UUID admissionId) {
        Admission admission = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new RuntimeException("Admission not found."));

        if (admission.getStatus() == AdmissionStatus.DISCHARGED) {
            throw new IllegalStateException("Patient already discharged.");
        }

        Bed bed = bedRepository.findById(admission.getBedId()).orElseThrow();
        bed.setStatus(BedStatus.CLEANING);
        bedRepository.save(bed);

        admission.setDischargeDate(LocalDateTime.now());
        admission.setStatus(AdmissionStatus.DISCHARGED);
        Admission savedAdmission = admissionRepository.save(admission);

        try {
            PatientDischargedEvent event = new PatientDischargedEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setPatientId(admission.getPatientId());
            event.setDoctorId(admission.getDoctorId());
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

        eventPublisher.publishEvent(new AdmissionUpdatedEvent(savedAdmission, Instant.now()));

        return savedAdmission;
    }

}
