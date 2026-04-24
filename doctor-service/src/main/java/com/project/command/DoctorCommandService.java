package com.project.command;

import com.project.dto.UpdateDoctorServiceResponseDto;
import com.project.dto.response.CreateDoctorServiceResponseDto;
import com.project.dto.response.LeaveResponseDto;
import com.project.dto.response.ShiftResponseDto;
import com.project.event.*;
import com.project.exception.ApiException;
import com.project.exception.DoctorNotFoundException;
import com.project.exception.EmailIsNotUniqueException;
import com.project.exception.PatientLimitException;
import com.project.helper.DoctorMapper;
import com.project.helper.DoctorValidator;
import com.project.model.*;
import com.project.repository.DoctorRepository;
import com.project.repository.LeaveAbsenceRepository;
import com.project.repository.ShiftRepository;
import com.project.utils.DateUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class DoctorCommandService {
    private final DoctorRepository doctorRepository;
    private final ShiftRepository shiftRepository;
    private final LeaveAbsenceRepository leaveAbsenceRepository;
    private final DoctorMapper doctorMapper;
    private final DoctorValidator doctorValidator;
    private final ApplicationEventPublisher eventPublisher;

    public DoctorCommandService(DoctorRepository doctorRepository, ShiftRepository shiftRepository, 
                                LeaveAbsenceRepository leaveAbsenceRepository, DoctorMapper doctorMapper, 
                                DoctorValidator doctorValidator, ApplicationEventPublisher eventPublisher) {
        this.doctorRepository = doctorRepository;
        this.shiftRepository = shiftRepository;
        this.leaveAbsenceRepository = leaveAbsenceRepository;
        this.doctorMapper = doctorMapper;
        this.doctorValidator = doctorValidator;
        this.eventPublisher = eventPublisher;
    }

    public CreateDoctorServiceResponseDto createDoctor(CreateDoctorCommand command) throws EmailIsNotUniqueException {
        doctorValidator.checkEmailIsUniqueOrNotForCreate(command.request(), doctorRepository);

        Doctor doctor = doctorMapper.toEntity(command.request());
        Doctor result = doctorRepository.save(doctor);

        eventPublisher.publishEvent(new DoctorCreatedEvent(result, Instant.now()));

        return doctorMapper.toCreateDoctorServiceResponseDto(result);
    }

    public UpdateDoctorServiceResponseDto updateDoctor(UpdateDoctorCommand command) throws DoctorNotFoundException {
        UUID id = command.id();
        Optional<Doctor> optionalDoctor = doctorRepository.findById(id);

        if (optionalDoctor.isPresent()) {
            Doctor existingDoctor = doctorMapper.getDoctorRequestDto(command.request(), optionalDoctor);
            doctorRepository.save(existingDoctor);

            eventPublisher.publishEvent(new DoctorUpdatedEvent(existingDoctor, Collections.singleton("PROFILE"), Instant.now()));

            return doctorMapper.getUpdateDoctorServiceResponseDto(existingDoctor);
        } else {
            throw new DoctorNotFoundException("Doctor with id " + id + " not found.");
        }
    }

    public void deleteDoctor(DeleteDoctorCommand command) {
        doctorRepository.deleteById(command.id());
        eventPublisher.publishEvent(new DoctorDeletedEvent(command.id(), Instant.now()));
    }

    public void increasePatientNumber(IncreasePatientNumberCommand command) throws PatientLimitException, DoctorNotFoundException {
        UUID id = command.id();
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new DoctorNotFoundException("Doctor not found: " + id));
        if (doctor.getPatientCount() >= doctor.getMaxPatientCount()) throw new PatientLimitException("Patient limit is full: " + doctor.getPatientCount() + "/" + doctor.getMaxPatientCount());
        doctor.setPatientCount(doctor.getPatientCount() + 1);
        doctorRepository.save(doctor);

        eventPublisher.publishEvent(new DoctorUpdatedEvent(doctor, Collections.singleton("PATIENT_COUNT"), Instant.now()));
    }

    public ShiftResponseDto createShift(CreateShiftCommand command) {
        UUID doctorId = command.doctorId();
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new DoctorNotFoundException("Doctor not found: " + doctorId));

        LocalDate shiftDate = DateUtils.parseDate(command.request().getShiftDate());
        LocalTime startTime = DateUtils.parseTime(command.request().getStartTime());
        LocalTime endTime = DateUtils.parseTime(command.request().getEndTime());
        
        if (!endTime.isAfter(startTime)) {
            throw new ApiException("INVALID_SLOT", "Shift end time must be after start time", 400);
        }

        Shift shift = doctorMapper.toShift(doctor.getId(), command.request(), shiftDate, startTime, endTime);
        shiftRepository.save(shift);

        eventPublisher.publishEvent(new DoctorShiftChangedEvent(doctorId, shift.getId(), DoctorShiftChangedEvent.ShiftAction.CREATED, Instant.now()));

        return doctorMapper.toShiftResponseDto(shift);
    }

    public void deleteShift(DeleteShiftCommand command) {
        UUID doctorId = command.doctorId();
        UUID shiftId = command.shiftId();
        
        doctorRepository.findById(doctorId)
                .orElseThrow(() -> new DoctorNotFoundException("Doctor not found: " + doctorId));
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ApiException("SHIFT_NOT_FOUND", "Shift not found: " + shiftId, 404));

        if (!shift.getDoctorId().equals(doctorId)) {
            throw new ApiException("SHIFT_NOT_FOUND", "Shift not found for doctor", 404);
        }

        shift.setStatus(ShiftStatus.CANCELLED);
        shiftRepository.save(shift);

        eventPublisher.publishEvent(new DoctorShiftChangedEvent(doctorId, shiftId, DoctorShiftChangedEvent.ShiftAction.STATUS_CHANGED, Instant.now()));
    }

    public LeaveResponseDto createLeave(CreateLeaveCommand command) {
        UUID doctorId = command.doctorId();
        doctorRepository.findById(doctorId)
                .orElseThrow(() -> new DoctorNotFoundException("Doctor not found: " + doctorId));

        LocalDateTime startDateTime = DateUtils.parseDateTime(command.request().getStartDateTime());
        LocalDateTime endDateTime = DateUtils.parseDateTime(command.request().getEndDateTime());
        
        if (!endDateTime.isAfter(startDateTime)) {
            throw new ApiException("INVALID_SLOT", "Leave end datetime must be after start datetime", 400);
        }

        LeaveAbsence leaveAbsence = doctorMapper.toLeaveAbsence(doctorId, command.request(), startDateTime, endDateTime);
        leaveAbsenceRepository.save(leaveAbsence);
        
        eventPublisher.publishEvent(new DoctorLeaveChangedEvent(doctorId, leaveAbsence.getId(), DoctorLeaveChangedEvent.LeaveAction.CREATED, Instant.now()));

        return doctorMapper.toLeaveResponseDto(leaveAbsence);
    }

    public LeaveResponseDto approveLeave(ApproveLeaveCommand command) {
        UUID doctorId = command.doctorId();
        UUID leaveId = command.leaveId();
        
        doctorRepository.findById(doctorId)
                .orElseThrow(() -> new DoctorNotFoundException("Doctor not found: " + doctorId));
        LeaveAbsence leaveAbsence = leaveAbsenceRepository.findById(leaveId)
                .orElseThrow(() -> new ApiException("LEAVE_NOT_FOUND", "Leave not found: " + leaveId, 404));

        if (!leaveAbsence.getDoctorId().equals(doctorId)) {
            throw new ApiException("LEAVE_NOT_FOUND", "Leave not found for doctor", 404);
        }

        leaveAbsence.setStatus(LeaveStatus.APPROVED);
        leaveAbsenceRepository.save(leaveAbsence);
        
        eventPublisher.publishEvent(new DoctorLeaveChangedEvent(doctorId, leaveId, DoctorLeaveChangedEvent.LeaveAction.APPROVED, Instant.now()));

        return doctorMapper.toLeaveResponseDto(leaveAbsence);
    }

    public void deleteLeave(DeleteLeaveCommand command) {
        UUID doctorId = command.doctorId();
        UUID leaveId = command.leaveId();
        
        doctorRepository.findById(doctorId)
                .orElseThrow(() -> new DoctorNotFoundException("Doctor not found: " + doctorId));
        LeaveAbsence leaveAbsence = leaveAbsenceRepository.findById(leaveId)
                .orElseThrow(() -> new ApiException("LEAVE_NOT_FOUND", "Leave not found: " + leaveId, 404));

        if (!leaveAbsence.getDoctorId().equals(doctorId)) {
            throw new ApiException("LEAVE_NOT_FOUND", "Leave not found for doctor", 404);
        }

        leaveAbsence.setStatus(LeaveStatus.CANCELLED);
        leaveAbsenceRepository.save(leaveAbsence);

        eventPublisher.publishEvent(new DoctorLeaveChangedEvent(doctorId, leaveId, DoctorLeaveChangedEvent.LeaveAction.CANCELLED, Instant.now()));
    }
}

