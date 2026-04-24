package com.project.command;

import com.project.dto.UpdateDoctorServiceRequestDto;
import com.project.dto.UpdateDoctorServiceResponseDto;
import com.project.dto.request.CreateDoctorServiceRequestDto;
import com.project.dto.response.CreateDoctorServiceResponseDto;
import com.project.event.DoctorCreatedEvent;
import com.project.event.DoctorUpdatedEvent;
import com.project.exception.DoctorNotFoundException;
import com.project.exception.EmailIsNotUniqueException;
import com.project.exception.PatientLimitException;
import com.project.helper.DoctorMapper;
import com.project.helper.DoctorValidator;
import com.project.model.Doctor;
import com.project.repository.DoctorRepository;
import com.project.repository.LeaveAbsenceRepository;
import com.project.repository.ShiftRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DoctorCommandServiceTest {

    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private ShiftRepository shiftRepository;
    @Mock
    private LeaveAbsenceRepository leaveAbsenceRepository;
    @Mock
    private DoctorMapper doctorMapper;
    @Mock
    private DoctorValidator doctorValidator;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DoctorCommandService doctorCommandService;

    @Test
    public void createDoctor_ShouldSaveAndPublishEvent() throws EmailIsNotUniqueException {
        CreateDoctorServiceRequestDto requestDto = new CreateDoctorServiceRequestDto();
        CreateDoctorCommand command = new CreateDoctorCommand(requestDto);
        Doctor doctor = new Doctor();
        doctor.setId(UUID.randomUUID());
        CreateDoctorServiceResponseDto responseDto = new CreateDoctorServiceResponseDto();

        when(doctorMapper.toEntity(requestDto)).thenReturn(doctor);
        when(doctorRepository.save(doctor)).thenReturn(doctor);
        when(doctorMapper.toCreateDoctorServiceResponseDto(doctor)).thenReturn(responseDto);

        CreateDoctorServiceResponseDto result = doctorCommandService.createDoctor(command);

        assertThat(result).isEqualTo(responseDto);
        verify(doctorValidator).checkEmailIsUniqueOrNotForCreate(requestDto, doctorRepository);
        verify(doctorRepository).save(doctor);
        verify(eventPublisher).publishEvent(any(DoctorCreatedEvent.class));
    }

    @Test
    public void updateDoctor_ShouldUpdateAndPublishEvent() throws DoctorNotFoundException {
        UUID id = UUID.randomUUID();
        UpdateDoctorServiceRequestDto requestDto = new UpdateDoctorServiceRequestDto();
        UpdateDoctorCommand command = new UpdateDoctorCommand(id, requestDto);
        Doctor existingDoctor = new Doctor();
        existingDoctor.setId(id);
        UpdateDoctorServiceResponseDto responseDto = new UpdateDoctorServiceResponseDto();

        when(doctorRepository.findById(id)).thenReturn(Optional.of(existingDoctor));
        when(doctorMapper.getDoctorRequestDto(eq(requestDto), any())).thenReturn(existingDoctor);
        when(doctorMapper.getUpdateDoctorServiceResponseDto(existingDoctor)).thenReturn(responseDto);

        UpdateDoctorServiceResponseDto result = doctorCommandService.updateDoctor(command);

        assertThat(result).isEqualTo(responseDto);
        verify(doctorRepository).save(existingDoctor);
        verify(eventPublisher).publishEvent(any(DoctorUpdatedEvent.class));
    }

    @Test
    public void increasePatientNumber_ShouldIncrementWhenBelowLimit() throws DoctorNotFoundException, PatientLimitException {
        UUID id = UUID.randomUUID();
        IncreasePatientNumberCommand command = new IncreasePatientNumberCommand(id);
        Doctor doctor = new Doctor();
        doctor.setId(id);
        doctor.setPatientCount(5);
        doctor.setMaxPatientCount(10);

        when(doctorRepository.findById(id)).thenReturn(Optional.of(doctor));

        doctorCommandService.increasePatientNumber(command);

        assertThat(doctor.getPatientCount()).isEqualTo(6);
        verify(doctorRepository).save(doctor);
        verify(eventPublisher).publishEvent(any(DoctorUpdatedEvent.class));
    }

    @Test
    public void increasePatientNumber_ShouldThrowExceptionWhenAtLimit() {
        UUID id = UUID.randomUUID();
        IncreasePatientNumberCommand command = new IncreasePatientNumberCommand(id);
        Doctor doctor = new Doctor();
        doctor.setId(id);
        doctor.setPatientCount(10);
        doctor.setMaxPatientCount(10);

        when(doctorRepository.findById(id)).thenReturn(Optional.of(doctor));

        assertThatThrownBy(() -> doctorCommandService.increasePatientNumber(command))
                .isInstanceOf(PatientLimitException.class)
                .hasMessageContaining("Patient limit is full");

        verify(doctorRepository, never()).save(any());
    }

    @Test
    public void increasePatientNumber_ShouldThrowExceptionWhenDoctorNotFound() {
        UUID id = UUID.randomUUID();
        IncreasePatientNumberCommand command = new IncreasePatientNumberCommand(id);

        when(doctorRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> doctorCommandService.increasePatientNumber(command))
                .isInstanceOf(DoctorNotFoundException.class)
                .hasMessageContaining("Doctor not found");
    }
}
