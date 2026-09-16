package com.project.admission_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.admission_service.model.*;
import com.project.admission_service.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MidnightChargeJobTest {

    @Mock
    private AdmissionRepository admissionRepository;
    @Mock
    private WardRepository wardRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private BedRepository bedRepository;
    @Mock
    private AdmissionOutboxRepository outboxRepository;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MidnightChargeJob midnightChargeJob;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void generateDailyCharges_Success() throws JsonProcessingException {
        UUID admissionId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID bedId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID wardId = UUID.randomUUID();

        Admission admission = new Admission();
        admission.setId(admissionId);
        admission.setPatientId(patientId);
        admission.setBedId(bedId);
        admission.setStatus(AdmissionStatus.ACTIVE);

        Bed bed = new Bed();
        bed.setId(bedId);
        bed.setRoomId(roomId);

        Room room = new Room();
        room.setId(roomId);
        room.setWardId(wardId);

        Ward ward = new Ward();
        ward.setId(wardId);
        ward.setDailyRate(BigDecimal.valueOf(1500));

        when(admissionRepository.findByStatus(AdmissionStatus.ACTIVE)).thenReturn(Collections.singletonList(admission));
        when(bedRepository.findById(bedId)).thenReturn(Optional.of(bed));
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(wardRepository.findById(wardId)).thenReturn(Optional.of(ward));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"amount\":1500}");

        midnightChargeJob.generateDailyCharges();

        verify(outboxRepository, times(1)).save(any(AdmissionOutboxEvent.class));
    }
}
