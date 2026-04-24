package com.project.patient_service.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.patient_service.dto.request.CreatePatientServiceRequestDto;
import com.project.patient_service.dto.request.KafkaPatientRequestDto;
import com.project.patient_service.dto.request.UpdatePatientServiceRequestDto;
import com.project.patient_service.dto.response.CreatePatientServiceResponseDto;
import com.project.patient_service.dto.response.UpdatePatientServiceResponseDto;
import com.project.patient_service.event.PatientCreatedEvent;
import com.project.patient_service.event.PatientDeletedEvent;
import com.project.patient_service.event.PatientUpdatedEvent;
import com.project.patient_service.exception.EmailAlreadyExistsException;
import com.project.patient_service.helper.UserMapper;
import com.project.patient_service.helper.UserValidator;
import com.project.patient_service.model.Patient;
import com.project.patient_service.repository.PatientOutboxRepository;
import com.project.patient_service.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PatientCommandServiceTest {

    @Mock
    private PatientRepository patientRepository;
    @Mock
    private PatientOutboxRepository patientOutboxRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserValidator userValidator;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PatientCommandService patientCommandService;

    @Test
    public void createPatient_ShouldSanitizeAndSave() throws EmailAlreadyExistsException, JsonProcessingException {
        CreatePatientServiceRequestDto requestDto = new CreatePatientServiceRequestDto();
        requestDto.setName("<script>alert('xss')</script>John");
        requestDto.setAddress("<b>Main St</b>");
        CreatePatientCommand command = new CreatePatientCommand(requestDto);
        
        Patient patient = new Patient();
        patient.setId(UUID.randomUUID());
        patient.setName("John");
        
        when(userValidator.getPatientForCreatePatient(any())).thenReturn(patient);
        when(patientRepository.save(any())).thenReturn(patient);
        when(userMapper.getKafkaPatientRequestDto(any())).thenReturn(new KafkaPatientRequestDto());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(userMapper.getCreatePatientServiceResponseDto(any())).thenReturn(new CreatePatientServiceResponseDto());

        patientCommandService.createPatient(command);

        assertThat(requestDto.getName()).isEqualTo("John");
        assertThat(requestDto.getAddress()).isEqualTo("Main St");
        
        verify(patientRepository).save(any());
        verify(patientOutboxRepository).save(any());
        verify(eventPublisher).publishEvent(any(PatientCreatedEvent.class));
    }

    @Test
    public void createPatient_WhenEmailExists_ShouldThrowException() throws EmailAlreadyExistsException {
        CreatePatientServiceRequestDto requestDto = new CreatePatientServiceRequestDto();
        CreatePatientCommand command = new CreatePatientCommand(requestDto);
        
        doThrow(new EmailAlreadyExistsException("Email exists")).when(userValidator).CheckEmailIsExistsOrNotForCreatePatient(any(), any());

        assertThatThrownBy(() -> patientCommandService.createPatient(command))
                .isInstanceOf(EmailAlreadyExistsException.class);
        
        verify(patientRepository, never()).save(any());
    }

    @Test
    public void updatePatient_ShouldSanitizeAndUpdate() throws JsonProcessingException {
        UUID id = UUID.randomUUID();
        UpdatePatientServiceRequestDto updateDto = new UpdatePatientServiceRequestDto();
        updateDto.setName("<u>New Name</u>");
        UpdatePatientCommand command = new UpdatePatientCommand(id, updateDto);
        
        Patient existingPatient = new Patient();
        existingPatient.setId(id);

        when(userValidator.getPatientForUpdateMethod(eq(id), any())).thenReturn(existingPatient);
        when(patientRepository.save(any())).thenReturn(existingPatient);
        when(userMapper.getKafkaPatientRequestDto(any())).thenReturn(new KafkaPatientRequestDto());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(userMapper.getUpdatePatientServiceResponseDto(any())).thenReturn(new UpdatePatientServiceResponseDto());

        patientCommandService.updatePatient(command);

        assertThat(updateDto.getName()).isEqualTo("New Name");
        verify(patientRepository).save(existingPatient);
        verify(patientOutboxRepository).save(any());
        verify(eventPublisher).publishEvent(any(PatientUpdatedEvent.class));
    }

    @Test
    public void deletePatient_ShouldSaveOutboxAndDelete() throws JsonProcessingException {
        UUID id = UUID.randomUUID();
        DeletePatientCommand command = new DeletePatientCommand(id);

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        patientCommandService.deletePatient(command);

        verify(patientOutboxRepository).save(any());
        verify(patientRepository).deleteById(id);
        verify(eventPublisher).publishEvent(any(PatientDeletedEvent.class));
    }
}
