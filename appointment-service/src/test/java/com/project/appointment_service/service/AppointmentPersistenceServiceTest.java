package com.project.appointment_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.appointment_service.dto.DoctorInfoDTO;
import com.project.appointment_service.dto.PatientInfoDTO;
import com.project.appointment_service.dto.request.CreateAppointmentServiceRequestDto;
import com.project.appointment_service.helper.AppointmentMapper;
import com.project.appointment_service.model.Appointment;
import com.project.appointment_service.model.AppointmentStatus;
import com.project.appointment_service.repository.AppointmentOutboxRepository;
import com.project.appointment_service.repository.AppointmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AppointmentPersistenceServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private AppointmentOutboxRepository outboxRepository;
    @Mock
    private AppointmentMapper appointmentMapper;
    @Mock
    private AppointmentSummaryService appointmentSummaryService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AppointmentPersistenceService persistenceService;

    @Test
    public void persistAppointmentAndOutbox_ShouldSaveAllCorrectly() throws JsonProcessingException {
        // Arrange
        CreateAppointmentServiceRequestDto request = new CreateAppointmentServiceRequestDto();
        PatientInfoDTO patientInfo = new PatientInfoDTO();
        patientInfo.setId(UUID.randomUUID().toString());
        patientInfo.setEmail("patient@test.com");
        DoctorInfoDTO doctorInfo = new DoctorInfoDTO();
        doctorInfo.setId(UUID.randomUUID().toString());

        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setAmount(100);

        when(appointmentMapper.getAppointment(request)).thenReturn(appointment);
        when(appointmentRepository.save(appointment)).thenReturn(appointment);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        Appointment saved = persistenceService.persistAppointmentAndOutbox(request, patientInfo, doctorInfo);

        // Assert
        assertThat(saved).isEqualTo(appointment);
        assertThat(saved.getStatus()).isEqualTo(AppointmentStatus.PAYMENT_PENDING);
        
        verify(appointmentRepository).save(appointment);
        verify(appointmentSummaryService).createOrUpdateSummary(appointment, patientInfo, doctorInfo);
        verify(outboxRepository).save(any());
    }

    @Test
    public void persistAppointmentAndOutbox_WhenSerializationFails_ShouldThrowException() throws JsonProcessingException {
        // Arrange
        CreateAppointmentServiceRequestDto request = new CreateAppointmentServiceRequestDto();
        PatientInfoDTO patientInfo = new PatientInfoDTO();
        DoctorInfoDTO doctorInfo = new DoctorInfoDTO();
        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());

        when(appointmentMapper.getAppointment(request)).thenReturn(appointment);
        when(appointmentRepository.save(appointment)).thenReturn(appointment);
        
        // Mock JsonProcessingException (it's checked, so we need to mock it properly)
        when(objectMapper.writeValueAsString(any())).thenThrow(new com.fasterxml.jackson.core.JsonParseException(null, "Test error"));

        // Act & Assert
        assertThatThrownBy(() -> persistenceService.persistAppointmentAndOutbox(request, patientInfo, doctorInfo))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Creation failed due to outbox error");
        
        verify(outboxRepository, never()).save(any());
    }

    @Test
    public void method_ShouldHaveTransactionalAnnotation() throws NoSuchMethodException {
        Method method = AppointmentPersistenceService.class.getMethod("persistAppointmentAndOutbox", 
                CreateAppointmentServiceRequestDto.class, PatientInfoDTO.class, DoctorInfoDTO.class);
        
        assertThat(method.isAnnotationPresent(Transactional.class)).isTrue();
    }
}
