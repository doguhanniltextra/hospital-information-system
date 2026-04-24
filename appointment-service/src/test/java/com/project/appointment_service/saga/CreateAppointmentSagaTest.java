package com.project.appointment_service.saga;

import com.project.appointment_service.dto.DoctorAvailabilityResponseDTO;
import com.project.appointment_service.dto.DoctorInfoDTO;
import com.project.appointment_service.dto.PatientInfoDTO;
import com.project.appointment_service.dto.request.CreateAppointmentServiceRequestDto;
import com.project.appointment_service.dto.response.CreateAppointmentServiceResponseDto;
import com.project.appointment_service.exception.CustomConflictException;
import com.project.appointment_service.helper.AppointmentMapper;
import com.project.appointment_service.model.Appointment;
import com.project.appointment_service.model.AppointmentStatus;
import com.project.appointment_service.repository.AppointmentRepository;
import com.project.appointment_service.service.AppointmentPersistenceService;
import com.project.appointment_service.utils.IdValidation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateAppointmentSagaTest {

    @Mock
    private IdValidation idValidation;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private AppointmentMapper appointmentMapper;
    @Mock
    private AppointmentPersistenceService persistenceService;

    @InjectMocks
    private CreateAppointmentSaga saga;

    @Test
    public void execute_ShouldSucceed_WhenAllChecksPass() {
        // Arrange
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        CreateAppointmentServiceRequestDto request = new CreateAppointmentServiceRequestDto();
        request.setPatientId(patientId);
        request.setDoctorId(doctorId);
        request.setServiceDate("2026-05-24 10:00");
        request.setServiceDateEnd("2026-05-24 10:30");

        PatientInfoDTO patientInfo = new PatientInfoDTO();
        DoctorInfoDTO doctorInfo = new DoctorInfoDTO();
        DoctorAvailabilityResponseDTO availability = new DoctorAvailabilityResponseDTO();
        availability.setAvailable(true);

        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setStatus(AppointmentStatus.PAYMENT_PENDING);

        CreateAppointmentServiceResponseDto responseDto = new CreateAppointmentServiceResponseDto();

        when(idValidation.fetchPatientInfo(patientId)).thenReturn(patientInfo);
        when(idValidation.fetchDoctorInfo(doctorId)).thenReturn(doctorInfo);
        when(idValidation.checkDoctorAvailability(eq(doctorId), any(), any(), any())).thenReturn(availability);
        when(appointmentRepository.findOverlappingAppointments(eq(doctorId), any(), any())).thenReturn(Collections.emptyList());
        when(persistenceService.persistAppointmentAndOutbox(eq(request), eq(patientInfo), eq(doctorInfo))).thenReturn(appointment);
        when(appointmentMapper.getCreateAppointmentServiceResponseDto(request)).thenReturn(responseDto);

        // Act
        CreateAppointmentServiceResponseDto result = saga.execute(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.PAYMENT_PENDING.name());
        verify(persistenceService).persistAppointmentAndOutbox(request, patientInfo, doctorInfo);
    }

    @Test
    public void execute_ShouldThrowException_WhenPatientNotFound() {
        UUID patientId = UUID.randomUUID();
        CreateAppointmentServiceRequestDto request = new CreateAppointmentServiceRequestDto();
        request.setPatientId(patientId);

        when(idValidation.fetchPatientInfo(patientId)).thenReturn(null);

        assertThatThrownBy(() -> saga.execute(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Patient does not exist");
        
        verifyNoInteractions(persistenceService);
    }

    @Test
    public void execute_ShouldThrowConflict_WhenDoctorUnavailable() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        CreateAppointmentServiceRequestDto request = new CreateAppointmentServiceRequestDto();
        request.setPatientId(patientId);
        request.setDoctorId(doctorId);

        PatientInfoDTO patientInfo = new PatientInfoDTO();
        DoctorInfoDTO doctorInfo = new DoctorInfoDTO();
        DoctorAvailabilityResponseDTO availability = new DoctorAvailabilityResponseDTO();
        availability.setAvailable(false);
        availability.setReasonCode("ON_LEAVE");

        when(idValidation.fetchPatientInfo(patientId)).thenReturn(patientInfo);
        when(idValidation.fetchDoctorInfo(doctorId)).thenReturn(doctorInfo);
        when(idValidation.checkDoctorAvailability(eq(doctorId), any(), any(), any())).thenReturn(availability);

        assertThatThrownBy(() -> saga.execute(request))
                .isInstanceOf(CustomConflictException.class)
                .hasMessageContaining("Doctor is not available")
                .hasMessageContaining("ON_LEAVE");

        verifyNoInteractions(persistenceService);
    }

    @Test
    public void execute_ShouldThrowConflict_WhenOverlappingDetected() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        CreateAppointmentServiceRequestDto request = new CreateAppointmentServiceRequestDto();
        request.setPatientId(patientId);
        request.setDoctorId(doctorId);

        PatientInfoDTO patientInfo = new PatientInfoDTO();
        DoctorInfoDTO doctorInfo = new DoctorInfoDTO();
        DoctorAvailabilityResponseDTO availability = new DoctorAvailabilityResponseDTO();
        availability.setAvailable(true);

        when(idValidation.fetchPatientInfo(patientId)).thenReturn(patientInfo);
        when(idValidation.fetchDoctorInfo(doctorId)).thenReturn(doctorInfo);
        when(idValidation.checkDoctorAvailability(eq(doctorId), any(), any(), any())).thenReturn(availability);
        when(appointmentRepository.findOverlappingAppointments(eq(doctorId), any(), any()))
                .thenReturn(Collections.singletonList(new Appointment()));

        assertThatThrownBy(() -> saga.execute(request))
                .isInstanceOf(CustomConflictException.class)
                .hasMessageContaining("Time slot overlaps");

        verifyNoInteractions(persistenceService);
    }
}
