package com.project.admission_service.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.admission_service.dto.AdmissionRequest;
import com.project.admission_service.event.*;
import com.project.admission_service.grpc.DoctorGrpcClient;
import com.project.admission_service.grpc.PatientGrpcClient;
import com.project.admission_service.model.*;
import com.project.admission_service.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdmissionCommandServiceTest {

    @Mock
    private WardRepository wardRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private BedRepository bedRepository;
    @Mock
    private AdmissionRepository admissionRepository;
    @Mock
    private AdmissionOutboxRepository outboxRepository;
    @Mock
    private AdmissionProcessedEventRepository processedEventRepository;
    @Mock
    private PatientGrpcClient patientGrpcClient;
    @Mock
    private DoctorGrpcClient doctorGrpcClient;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AdmissionCommandService admissionCommandService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void admitPatient_Success() {
        // Arrange
        UUID wardId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID bedId = UUID.randomUUID();

        AdmissionRequest request = new AdmissionRequest();
        request.setPatientId(patientId);
        request.setDoctorId(doctorId);
        request.setWardId(wardId);

        Bed bed = new Bed();
        bed.setId(bedId);
        bed.setRoomId(roomId);
        bed.setBedNumber("B1");
        bed.setStatus(BedStatus.EMPTY);

        when(patientGrpcClient.existsById(patientId)).thenReturn(true);
        when(doctorGrpcClient.existsById(doctorId)).thenReturn(true);
        when(bedRepository.findFirstEmptyBedInWardForUpdate(wardId)).thenReturn(Optional.of(bed));
        when(admissionRepository.save(any(Admission.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Admission result = admissionCommandService.admitPatient(request);

        // Assert
        assertNotNull(result);
        assertEquals(patientId, result.getPatientId());
        assertEquals(doctorId, result.getDoctorId());
        assertEquals(bedId, result.getBedId());
        assertEquals(BedStatus.OCCUPIED, bed.getStatus());
        assertEquals(AdmissionStatus.ACTIVE, result.getStatus());
        verify(bedRepository, times(1)).save(bed);
        verify(admissionRepository, times(1)).save(any(Admission.class));
        verify(eventPublisher, times(1)).publishEvent(any(AdmissionCreatedEvent.class));
    }

    @Test
    void admitPatient_NoBedsAvailable_ThrowsException() {
        // Arrange
        UUID wardId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        AdmissionRequest request = new AdmissionRequest();
        request.setPatientId(patientId);
        request.setDoctorId(doctorId);
        request.setWardId(wardId);

        when(patientGrpcClient.existsById(patientId)).thenReturn(true);
        when(doctorGrpcClient.existsById(doctorId)).thenReturn(true);
        when(bedRepository.findFirstEmptyBedInWardForUpdate(wardId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> admissionCommandService.admitPatient(request));
        assertEquals("No available beds in the selected ward.", exception.getMessage());
    }

    @Test
    void dischargePatient_Success() throws JsonProcessingException {
        // Arrange
        UUID admissionId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID bedId = UUID.randomUUID();

        Admission admission = new Admission();
        admission.setId(admissionId);
        admission.setPatientId(patientId);
        admission.setDoctorId(doctorId);
        admission.setBedId(bedId);
        admission.setStatus(AdmissionStatus.ACTIVE);

        Bed bed = new Bed();
        bed.setId(bedId);
        bed.setStatus(BedStatus.OCCUPIED);

        when(admissionRepository.findById(admissionId)).thenReturn(Optional.of(admission));
        when(bedRepository.findById(bedId)).thenReturn(Optional.of(bed));
        when(admissionRepository.save(any(Admission.class))).thenAnswer(i -> i.getArguments()[0]);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        Admission result = admissionCommandService.dischargePatient(admissionId);

        // Assert
        assertNotNull(result);
        assertEquals(AdmissionStatus.DISCHARGED, result.getStatus());
        assertEquals(BedStatus.CLEANING, bed.getStatus());
        assertNotNull(result.getDischargeDate());
        verify(bedRepository, times(1)).save(bed);
        verify(outboxRepository, times(1)).save(any(AdmissionOutboxEvent.class));
        verify(eventPublisher, times(1)).publishEvent(any(AdmissionUpdatedEvent.class));
    }

    @Test
    void dischargePatient_NotFound_ThrowsException() {
        // Arrange
        UUID admissionId = UUID.randomUUID();
        when(admissionRepository.findById(admissionId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> admissionCommandService.dischargePatient(admissionId));
        assertEquals("Admission not found.", exception.getMessage());
    }

    @Test
    void dischargePatient_AlreadyDischarged_ThrowsException() {
        // Arrange
        UUID admissionId = UUID.randomUUID();
        Admission admission = new Admission();
        admission.setStatus(AdmissionStatus.DISCHARGED);
        when(admissionRepository.findById(admissionId)).thenReturn(Optional.of(admission));

        // Act & Assert
        Exception exception = assertThrows(IllegalStateException.class, () -> admissionCommandService.dischargePatient(admissionId));
        assertEquals("Patient already discharged.", exception.getMessage());
    }
}

