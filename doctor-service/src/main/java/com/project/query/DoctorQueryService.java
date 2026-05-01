package com.project.query;

import com.project.dto.response.AvailabilityResponseDto;
import com.project.dto.response.DoctorAvailabilitySummaryDto;
import com.project.dto.response.ShiftResponseDto;
import com.project.exception.ApiException;
import com.project.exception.DoctorNotFoundException;
import com.project.helper.DoctorMapper;
import com.project.model.*;
import com.project.repository.DoctorRepository;
import com.project.repository.LeaveAbsenceRepository;
import com.project.repository.ShiftRepository;
import com.project.utils.DateUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class for handling read-only operations and availability checks for Doctors.
 * Provides functionality to list doctors, shifts, and verify availability for specific time slots.
 */
@Service
@Transactional(readOnly = true)
public class DoctorQueryService {
    private final DoctorRepository doctorRepository;
    private final ShiftRepository shiftRepository;
    private final LeaveAbsenceRepository leaveAbsenceRepository;
    private final DoctorMapper doctorMapper;

    /**
     * Initializes the service with required repositories and mappers.
     * 
     * @param doctorRepository Repository for Doctor entity access
     * @param shiftRepository Repository for Shift entity access
     * @param leaveAbsenceRepository Repository for LeaveAbsence entity access
     * @param doctorMapper Mapper for DTO/Entity transformations
     */
    public DoctorQueryService(DoctorRepository doctorRepository, ShiftRepository shiftRepository, 
                              LeaveAbsenceRepository leaveAbsenceRepository, DoctorMapper doctorMapper) {
        this.doctorRepository = doctorRepository;
        this.shiftRepository = shiftRepository;
        this.leaveAbsenceRepository = leaveAbsenceRepository;
        this.doctorMapper = doctorMapper;
    }

    /**
     * Retrieves a doctor by their unique identifier.
     * 
     * @param query Query object containing the doctor ID
     * @return An Optional containing the doctor if found, or empty otherwise
     */
    public Optional<Doctor> findDoctorById(GetDoctorQuery query) {
        return doctorRepository.findById(query.id());
    }

    /**
     * Retrieves a paginated list of doctors.
     * 
     * @param query Query object containing pagination and filtering parameters
     * @return A Page of Doctor entities
     */
    public Page<Doctor> getDoctors(GetDoctorsQuery query) {
        // Current implementation doesn't use the specialization filter in the repository call yet, 
        // but we can add it if needed. For now, we follow the legacy logic.
        return doctorRepository.findAll(query.pageable());
    }

    /**
     * Lists active shifts for a specific doctor within a date range.
     * 
     * @param doctorId The unique ID of the doctor
     * @param fromDate Start date of the range
     * @param toDate End date of the range
     * @return A list of Shift response DTOs
     * @throws DoctorNotFoundException If the doctor ID is invalid
     */
    public List<ShiftResponseDto> listShifts(UUID doctorId, LocalDate fromDate, LocalDate toDate) {
        doctorRepository.findById(doctorId)
                .orElseThrow(() -> new DoctorNotFoundException("Doctor not found: " + doctorId));

        return shiftRepository.findByDoctorIdAndShiftDateBetweenAndStatus(doctorId, fromDate, toDate, ShiftStatus.ACTIVE)
                .stream()
                .map(doctorMapper::toShiftResponseDto)
                .toList();
    }

    /**
     * Checks if a doctor is available for a requested time slot and service type.
     * Considers active shifts, approved leaves, and surgery-specific constraints.
     * 
     * @param doctorId The unique ID of the doctor
     * @param query Query containing requested start/end times and service type
     * @return Availability response detailing whether the doctor is free and why
     * @throws DoctorNotFoundException If the doctor ID is invalid
     * @throws ApiException If time slot parameters are invalid
     */
    public AvailabilityResponseDto checkDoctorAvailability(UUID doctorId, GetAvailableDoctorsQuery query) {
        doctorRepository.findById(doctorId)
                .orElseThrow(() -> new DoctorNotFoundException("Doctor not found: " + doctorId));

        LocalDateTime startDateTime = DateUtils.parseDateTime(query.start());
        LocalDateTime endDateTime = DateUtils.parseDateTime(query.end());

        if (!endDateTime.isAfter(startDateTime)) {
            throw new ApiException("INVALID_SLOT", "Appointment end must be after start", 400);
        }

        if (!isWithinShift(doctorId, startDateTime, endDateTime)) {
            return new AvailabilityResponseDto(false, "OUTSIDE_SHIFT", "Requested slot is outside active shift hours");
        }

        if (hasLeaveConflict(doctorId, startDateTime, endDateTime)) {
            return new AvailabilityResponseDto(false, "ON_LEAVE", "Doctor is on approved leave");
        }

        if (query.serviceType() == ServiceType.SURGERY && hasSurgeryConflict(doctorId, startDateTime, endDateTime)) {
            return new AvailabilityResponseDto(false, "SURGERY_CONFLICT", "Surgery slot conflicts with surgery shift");
        }

        return new AvailabilityResponseDto(true, "IN_SHIFT", "Doctor is available for requested slot");
    }

    /**
     * Lists all doctors with their availability status for a specific time slot.
     * 
     * @param query Query containing the requested slot
     * @param specializationFilterStr Optional specialization filter
     * @param pageable Pagination details
     * @return A paginated list of doctor availability summaries
     * @throws ApiException If time slot or specialization parameters are invalid
     */
    public Page<DoctorAvailabilitySummaryDto> getAvailableDoctors(GetAvailableDoctorsQuery query, String specializationFilterStr, Pageable pageable) {
        LocalDateTime startDateTime = DateUtils.parseDateTime(query.start());
        LocalDateTime endDateTime = DateUtils.parseDateTime(query.end());

        if (!endDateTime.isAfter(startDateTime)) {
            throw new ApiException("INVALID_SLOT", "Appointment end must be after start", 400);
        }

        Specialization specializationFilter = null;
        if (specializationFilterStr != null && !specializationFilterStr.isBlank()) {
            try {
                specializationFilter = Specialization.valueOf(specializationFilterStr);
            } catch (IllegalArgumentException ex) {
                throw new ApiException("INVALID_SPECIALIZATION", "Invalid specialization value", 400);
            }
        }

        final Specialization finalSpecializationFilter = specializationFilter;
        return doctorRepository.findAll(pageable)
                .map(doctor -> {
                    boolean specializationMatches = finalSpecializationFilter == null || doctor.getSpecialization() == finalSpecializationFilter;
                    boolean available = false;
                    if (specializationMatches) {
                        AvailabilityResponseDto result = checkDoctorAvailability(
                                doctor.getId(),
                                query
                        );
                        available = result.isAvailable();
                    }

                    return doctorMapper.toDoctorAvailabilitySummaryDto(doctor, available);
                });
    }

    /**
     * Checks if a requested slot falls entirely within a doctor's active shift.
     */
    private boolean isWithinShift(UUID doctorId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        LocalDate shiftDate = startDateTime.toLocalDate();
        if (!shiftDate.equals(endDateTime.toLocalDate())) {
            return false;
        }
        List<Shift> shifts = shiftRepository.findByDoctorIdAndShiftDateAndStatus(doctorId, shiftDate, ShiftStatus.ACTIVE);
        return shifts.stream().anyMatch(shift ->
                !startDateTime.toLocalTime().isBefore(shift.getStartTime())
                        && !endDateTime.toLocalTime().isAfter(shift.getEndTime()));
    }

    /**
     * Checks if a requested slot overlaps with an approved leave.
     */
    private boolean hasLeaveConflict(UUID doctorId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return leaveAbsenceRepository.existsByDoctorIdAndStatusAndStartDateTimeLessThanEqualAndEndDateTimeGreaterThanEqual(
                doctorId,
                LeaveStatus.APPROVED,
                endDateTime,
                startDateTime
        );
    }

    /**
     * Checks if a surgery slot conflicts with an existing surgery shift.
     */
    private boolean hasSurgeryConflict(UUID doctorId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        LocalDate shiftDate = startDateTime.toLocalDate();
        if (!shiftDate.equals(endDateTime.toLocalDate())) {
            return true;
        }
        List<Shift> surgeryShifts = shiftRepository.findByDoctorIdAndShiftDateAndShiftTypeAndStatus(
                doctorId, shiftDate, com.project.model.ShiftType.SURGERY, ShiftStatus.ACTIVE
        );
        java.time.LocalTime startTime = startDateTime.toLocalTime();
        java.time.LocalTime endTime = endDateTime.toLocalTime();
        return surgeryShifts.stream().anyMatch(shift ->
                !startTime.isAfter(shift.getEndTime()) && !endTime.isBefore(shift.getStartTime()));
    }
}
