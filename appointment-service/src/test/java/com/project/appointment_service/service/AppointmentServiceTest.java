package com.project.appointment_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.appointment_service.dto.PatientInfoDTO;
import com.project.appointment_service.dto.request.CreateAppointmentServiceRequestDto;
import com.project.appointment_service.dto.response.CreateAppointmentServiceResponseDto;
import com.project.appointment_service.exception.CustomNotFoundException;
import com.project.appointment_service.helper.AppointmentMapper;
import com.project.appointment_service.helper.AppointmentValidator;
import com.project.appointment_service.model.Appointment;
import com.project.appointment_service.model.AppointmentStatus;
import com.project.appointment_service.repository.AppointmentOutboxRepository;
import com.project.appointment_service.repository.AppointmentRepository;
import com.project.appointment_service.saga.CreateAppointmentSaga;
import com.project.appointment_service.utils.IdValidation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AppointmentServiceTest {

    @Mock
    private AppointmentOutboxRepository outboxRepository;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private IdValidation idValidation;
    @Mock
    private AppointmentSummaryService appointmentSummaryService;
    @Mock
    private AppointmentMapper appointmentMapper;
    @Mock
    private AppointmentValidator appointmentValidator;
    @Mock
    private CreateAppointmentSaga createAppointmentSaga;

    @InjectMocks
    private AppointmentService appointmentService;

    @Test
    public void createAppointment_ShouldDelegateToSaga() {
        CreateAppointmentServiceRequestDto request = new CreateAppointmentServiceRequestDto();
        CreateAppointmentServiceResponseDto expectedResponse = new CreateAppointmentServiceResponseDto();
        when(createAppointmentSaga.execute(request)).thenReturn(expectedResponse);

        CreateAppointmentServiceResponseDto actualResponse = appointmentService.createAppointment(request);

        assertThat(actualResponse).isEqualTo(expectedResponse);
        verify(createAppointmentSaga).execute(request);
    }

    @Test
    public void updateAppointment_ShouldUpdateAndReturnOk() {
        UUID id = UUID.randomUUID();
        Appointment appointment = new Appointment();
        appointment.setId(id);
        Appointment existing = new Appointment();
        existing.setId(id);

        when(appointmentRepository.findById(id)).thenReturn(Optional.of(existing));
        when(appointmentRepository.save(existing)).thenReturn(existing);

        ResponseEntity<Appointment> response = appointmentService.updateAppointment(appointment);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(existing);
        verify(appointmentMapper).updateAppointmentExtracted(appointment, existing);
        verify(appointmentSummaryService).createOrUpdateSummary(existing);
    }

    @Test
    public void updateAppointment_WhenNotFound_ShouldThrowException() {
        UUID id = UUID.randomUUID();
        Appointment appointment = new Appointment();
        appointment.setId(id);

        when(appointmentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.updateAppointment(appointment))
                .isInstanceOf(CustomNotFoundException.class)
                .hasMessageContaining("Appointment not found");
    }

    @Test
    public void deleteAppointment_ShouldCallRepositories() {
        UUID id = UUID.randomUUID();

        appointmentService.deleteAppointment(id);

        verify(appointmentRepository).deleteById(id);
        verify(appointmentSummaryService).deleteSummary(id);
    }

    @Test
    public void updatePaymentStatus_ShouldUpdateStatusAndOutbox() throws JsonProcessingException {
        UUID id = UUID.randomUUID();
        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setPatientId(UUID.randomUUID());
        appointment.setDoctorId(UUID.randomUUID());
        appointment.setAmount(100);
        appointment.setStatus(AppointmentStatus.PAYMENT_PENDING);

        PatientInfoDTO patientInfo = new PatientInfoDTO();
        patientInfo.setEmail("test@example.com");

        when(appointmentValidator.getAppointmentForUpdatePaymentStatus(eq(id), any())).thenReturn(appointment);
        when(idValidation.fetchPatientInfo(appointment.getPatientId())).thenReturn(patientInfo);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        appointmentService.updatePaymentStatus(id, true);

        assertThat(appointment.isPaymentStatus()).isTrue();
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.PAYMENT_CONFIRMED);
        verify(appointmentRepository).save(appointment);
        verify(appointmentSummaryService).updatePaymentStatus(id, true);
        verify(outboxRepository).save(any());
    }
}
