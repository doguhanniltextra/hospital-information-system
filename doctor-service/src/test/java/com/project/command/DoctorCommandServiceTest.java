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
    public void increasePatientNumber_ShouldIncrementWhenBelowLimit()
            throws DoctorNotFoundException, PatientLimitException {
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

    @Test
    public void deleteDoctor_ShouldDeleteAndPublishEvent() {
        UUID id = UUID.randomUUID();
        DeleteDoctorCommand command = new DeleteDoctorCommand(id);

        doctorCommandService.deleteDoctor(command);

        verify(doctorRepository).deleteById(id);
        verify(eventPublisher).publishEvent(any(com.project.event.DoctorDeletedEvent.class));
    }

    @Test
    public void createShift_ShouldSaveAndPublishEvent() {
        UUID doctorId = UUID.randomUUID();
        com.project.dto.request.CreateShiftRequestDto request = new com.project.dto.request.CreateShiftRequestDto();
        request.setShiftDate("2026-05-24");
        request.setStartTime("09:00");
        request.setEndTime("17:00");
        CreateShiftCommand command = new CreateShiftCommand(doctorId, request);

        Doctor doctor = new Doctor();
        doctor.setId(doctorId);
        com.project.model.Shift shift = new com.project.model.Shift();
        com.project.dto.response.ShiftResponseDto responseDto = new com.project.dto.response.ShiftResponseDto();

        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(doctorMapper.toShift(eq(doctorId), eq(request), any(), any(), any())).thenReturn(shift);
        when(doctorMapper.toShiftResponseDto(shift)).thenReturn(responseDto);

        com.project.dto.response.ShiftResponseDto result = doctorCommandService.createShift(command);

        assertThat(result).isEqualTo(responseDto);
        verify(shiftRepository).save(shift);
        verify(eventPublisher).publishEvent(any(com.project.event.DoctorShiftChangedEvent.class));
    }

    @Test
    public void createShift_WhenEndTimeBeforeStartTime_ShouldThrowException() {
        UUID doctorId = UUID.randomUUID();
        com.project.dto.request.CreateShiftRequestDto request = new com.project.dto.request.CreateShiftRequestDto();
        request.setShiftDate("2026-05-24");
        request.setStartTime("17:00");
        request.setEndTime("09:00");
        CreateShiftCommand command = new CreateShiftCommand(doctorId, request);

        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(new Doctor()));

        assertThatThrownBy(() -> doctorCommandService.createShift(command))
                .isInstanceOf(com.project.exception.ApiException.class)
                .hasMessageContaining("Shift end time must be after start time");
    }

    @Test
    public void deleteShift_ShouldUpdateStatusToCancelledAndPublishEvent() {
        UUID doctorId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        DeleteShiftCommand command = new DeleteShiftCommand(doctorId, shiftId);

        com.project.model.Shift shift = new com.project.model.Shift();
        shift.setId(shiftId);
        shift.setDoctorId(doctorId);

        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(new Doctor()));
        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(shift));

        doctorCommandService.deleteShift(command);

        assertThat(shift.getStatus()).isEqualTo(com.project.model.ShiftStatus.CANCELLED);
        verify(shiftRepository).save(shift);
        verify(eventPublisher).publishEvent(any(com.project.event.DoctorShiftChangedEvent.class));
    }

    @Test
    public void createLeave_ShouldSaveAndPublishEvent() {
        UUID doctorId = UUID.randomUUID();
        com.project.dto.request.CreateLeaveRequestDto request = new com.project.dto.request.CreateLeaveRequestDto();
        request.setStartDateTime("2026-05-24 09:00");
        request.setEndDateTime("2026-05-24 17:00");
        CreateLeaveCommand command = new CreateLeaveCommand(doctorId, request);

        com.project.model.LeaveAbsence leave = new com.project.model.LeaveAbsence();
        com.project.dto.response.LeaveResponseDto responseDto = new com.project.dto.response.LeaveResponseDto();

        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(new Doctor()));
        when(doctorMapper.toLeaveAbsence(eq(doctorId), eq(request), any(), any())).thenReturn(leave);
        when(doctorMapper.toLeaveResponseDto(leave)).thenReturn(responseDto);

        com.project.dto.response.LeaveResponseDto result = doctorCommandService.createLeave(command);

        assertThat(result).isEqualTo(responseDto);
        verify(leaveAbsenceRepository).save(leave);
        verify(eventPublisher).publishEvent(any(com.project.event.DoctorLeaveChangedEvent.class));
    }

    @Test
    public void approveLeave_ShouldUpdateStatusAndPublishEvent() {
        UUID doctorId = UUID.randomUUID();
        UUID leaveId = UUID.randomUUID();
        ApproveLeaveCommand command = new ApproveLeaveCommand(doctorId, leaveId);

        com.project.model.LeaveAbsence leave = new com.project.model.LeaveAbsence();
        leave.setId(leaveId);
        leave.setDoctorId(doctorId);
        com.project.dto.response.LeaveResponseDto responseDto = new com.project.dto.response.LeaveResponseDto();

        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(new Doctor()));
        when(leaveAbsenceRepository.findById(leaveId)).thenReturn(Optional.of(leave));
        when(doctorMapper.toLeaveResponseDto(leave)).thenReturn(responseDto);

        com.project.dto.response.LeaveResponseDto result = doctorCommandService.approveLeave(command);

        assertThat(result).isEqualTo(responseDto);
        assertThat(leave.getStatus()).isEqualTo(com.project.model.LeaveStatus.APPROVED);
        verify(leaveAbsenceRepository).save(leave);
        verify(eventPublisher).publishEvent(any(com.project.event.DoctorLeaveChangedEvent.class));
    }

    @Test
    public void deleteLeave_ShouldUpdateStatusToCancelledAndPublishEvent() {
        UUID doctorId = UUID.randomUUID();
        UUID leaveId = UUID.randomUUID();
        DeleteLeaveCommand command = new DeleteLeaveCommand(doctorId, leaveId);

        com.project.model.LeaveAbsence leave = new com.project.model.LeaveAbsence();
        leave.setId(leaveId);
        leave.setDoctorId(doctorId);

        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(new Doctor()));
        when(leaveAbsenceRepository.findById(leaveId)).thenReturn(Optional.of(leave));

        doctorCommandService.deleteLeave(command);

        assertThat(leave.getStatus()).isEqualTo(com.project.model.LeaveStatus.CANCELLED);
        verify(leaveAbsenceRepository).save(leave);
        verify(eventPublisher).publishEvent(any(com.project.event.DoctorLeaveChangedEvent.class));
    }
}
