package com.project.helper;

import com.project.dto.UpdateDoctorControllerResponseDto;
import com.project.dto.UpdateDoctorServiceRequestDto;
import com.project.dto.UpdateDoctorServiceResponseDto;
import com.project.dto.request.CreateLeaveRequestDto;
import com.project.dto.request.CreateDoctorControllerRequestDto;
import com.project.dto.request.CreateDoctorServiceRequestDto;
import com.project.dto.request.CreateShiftRequestDto;
import com.project.dto.response.DoctorAvailabilitySummaryDto;
import com.project.dto.response.CreateDoctorServiceResponseDto;
import com.project.dto.response.LeaveResponseDto;
import com.project.dto.response.ShiftResponseDto;
import com.project.model.Doctor;
import com.project.model.LeaveAbsence;
import com.project.model.LeaveStatus;
import com.project.model.Shift;
import com.project.model.ShiftStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Component
public class DoctorMapper {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public Doctor toEntity(CreateDoctorServiceRequestDto createDoctorServiceRequestDto) {
        Doctor doctor = new Doctor();
        doctor.setId(createDoctorServiceRequestDto.getId());
        doctor.setName(createDoctorServiceRequestDto.getName());
        doctor.setEmail(createDoctorServiceRequestDto.getEmail());
        doctor.setNumber(createDoctorServiceRequestDto.getNumber());

        doctor.setSpecialization(createDoctorServiceRequestDto.getSpecialization());
        doctor.setYearsOfExperience(createDoctorServiceRequestDto.getYearsOfExperience());
        doctor.setHospitalName(createDoctorServiceRequestDto.getHospitalName());
        doctor.setDepartment(createDoctorServiceRequestDto.getDepartment());
        doctor.setLicenseNumber(createDoctorServiceRequestDto.getLicenseNumber());
        doctor.setAvailable(createDoctorServiceRequestDto.isAvailable());
        doctor.setPatientCount(createDoctorServiceRequestDto.getPatientCount());
        doctor.setMaxPatientCount(createDoctorServiceRequestDto.getMaxPatientCount());
        doctor.setAuthUserId(createDoctorServiceRequestDto.getAuthUserId());
        return doctor;
    }

    public CreateDoctorServiceResponseDto toCreateDoctorServiceResponseDto(Doctor result) {
        CreateDoctorServiceResponseDto createDoctorServiceResponseDto = new CreateDoctorServiceResponseDto();
        createDoctorServiceResponseDto.setName(result.getName());
        createDoctorServiceResponseDto.setEmail(result.getEmail());
        createDoctorServiceResponseDto.setId(result.getId());
        createDoctorServiceResponseDto.setNumber(result.getNumber());
        createDoctorServiceResponseDto.setHospitalName(result.getHospitalName());
        return createDoctorServiceResponseDto;
    }

    public UpdateDoctorServiceResponseDto getUpdateDoctorServiceResponseDto(Doctor existingDoctor) {
        UpdateDoctorServiceResponseDto updateDoctorServiceResponseDto = new UpdateDoctorServiceResponseDto();
        updateDoctorServiceResponseDto.setId(existingDoctor.getId());
        updateDoctorServiceResponseDto.setName(existingDoctor.getName());
        updateDoctorServiceResponseDto.setEmail(existingDoctor.getEmail());
        updateDoctorServiceResponseDto.setNumber(existingDoctor.getNumber());
        updateDoctorServiceResponseDto.setSpecialization(existingDoctor.getSpecialization());
        updateDoctorServiceResponseDto.setYearsOfExperience(existingDoctor.getYearsOfExperience());
        updateDoctorServiceResponseDto.setHospitalName(existingDoctor.getHospitalName());
        updateDoctorServiceResponseDto.setDepartment(existingDoctor.getDepartment());
        updateDoctorServiceResponseDto.setLicenseNumber(existingDoctor.getLicenseNumber());
        updateDoctorServiceResponseDto.setAvailable(existingDoctor.isAvailable());
        updateDoctorServiceResponseDto.setPatientCount(existingDoctor.getPatientCount());
        updateDoctorServiceResponseDto.setGetMaximumPatient(existingDoctor.isGetMaximumPatient());
        updateDoctorServiceResponseDto.setMaxPatientCount(existingDoctor.getMaxPatientCount());
        return updateDoctorServiceResponseDto;
    }

    public Doctor getDoctorRequestDto(UpdateDoctorServiceRequestDto updateDoctorServiceRequestDto,
            Optional<Doctor> optionalDoctor) {
        Doctor existingDoctor = optionalDoctor.get();
        existingDoctor.setName(updateDoctorServiceRequestDto.getName());
        existingDoctor.setSpecialization(updateDoctorServiceRequestDto.getSpecialization());
        existingDoctor.setEmail(updateDoctorServiceRequestDto.getEmail());
        existingDoctor.setHospitalName(updateDoctorServiceRequestDto.getHospitalName());
        existingDoctor.setLicenseNumber(updateDoctorServiceRequestDto.getLicenseNumber());
        existingDoctor.setMaxPatientCount(updateDoctorServiceRequestDto.getMaxPatientCount());
        return existingDoctor;
    }

    public static UpdateDoctorControllerResponseDto getDoctorControllerResponseDto(
            UpdateDoctorServiceResponseDto updateDoctorServiceResponseDto) {
        UpdateDoctorControllerResponseDto updateDoctorControllerResponseDto = new UpdateDoctorControllerResponseDto();
        updateDoctorControllerResponseDto.setId(updateDoctorServiceResponseDto.getId());
        updateDoctorControllerResponseDto.setName(updateDoctorServiceResponseDto.getName());
        updateDoctorControllerResponseDto.setNumber(updateDoctorServiceResponseDto.getNumber());
        updateDoctorControllerResponseDto.setEmail(updateDoctorServiceResponseDto.getEmail());
        updateDoctorControllerResponseDto.setSpecialization(updateDoctorServiceResponseDto.getSpecialization());
        updateDoctorControllerResponseDto.setYearsOfExperience(updateDoctorServiceResponseDto.getYearsOfExperience());
        updateDoctorControllerResponseDto.setMaxPatientCount(updateDoctorServiceResponseDto.getMaxPatientCount());
        return updateDoctorControllerResponseDto;
    }

    public CreateDoctorServiceRequestDto getCreateDoctorServiceRequestDto(
            CreateDoctorControllerRequestDto createDoctorControllerRequestDto) {
        CreateDoctorServiceRequestDto createDoctorServiceRequestDto = new CreateDoctorServiceRequestDto();
        createDoctorServiceRequestDto.setId(createDoctorControllerRequestDto.getId());
        createDoctorServiceRequestDto.setName(createDoctorControllerRequestDto.getName());
        createDoctorServiceRequestDto.setEmail(createDoctorControllerRequestDto.getEmail());
        createDoctorServiceRequestDto.setNumber(createDoctorControllerRequestDto.getNumber());
        createDoctorServiceRequestDto.setSpecialization(createDoctorControllerRequestDto.getSpecialization());
        createDoctorServiceRequestDto.setYearsOfExperience(createDoctorControllerRequestDto.getYearsOfExperience());
        createDoctorServiceRequestDto.setHospitalName(createDoctorControllerRequestDto.getHospitalName());
        createDoctorServiceRequestDto.setDepartment(createDoctorControllerRequestDto.getDepartment());
        createDoctorServiceRequestDto.setLicenseNumber(createDoctorControllerRequestDto.getLicenseNumber());
        createDoctorServiceRequestDto.setAvailable(createDoctorControllerRequestDto.isAvailable());
        createDoctorServiceRequestDto.setPatientCount(createDoctorControllerRequestDto.getPatientCount());
        createDoctorServiceRequestDto.setMaxPatientCount(createDoctorControllerRequestDto.getMaxPatientCount());
        createDoctorServiceRequestDto.setAuthUserId(createDoctorControllerRequestDto.getAuthUserId());
        return createDoctorServiceRequestDto;
    }

    public Shift toShift(UUID doctorId, CreateShiftRequestDto requestDto, java.time.LocalDate shiftDate,
            java.time.LocalTime startTime, java.time.LocalTime endTime) {
        Shift shift = new Shift();
        shift.setDoctorId(doctorId);
        shift.setShiftDate(shiftDate);
        shift.setStartTime(startTime);
        shift.setEndTime(endTime);
        shift.setShiftType(requestDto.getShiftType());
        shift.setStatus(ShiftStatus.ACTIVE);
        return shift;
    }

    public ShiftResponseDto toShiftResponseDto(Shift shift) {
        ShiftResponseDto dto = new ShiftResponseDto();
        dto.setId(shift.getId());
        dto.setDoctorId(shift.getDoctorId());
        dto.setShiftDate(shift.getShiftDate().toString());
        dto.setStartTime(shift.getStartTime().toString());
        dto.setEndTime(shift.getEndTime().toString());
        dto.setShiftType(shift.getShiftType());
        dto.setStatus(shift.getStatus());
        return dto;
    }

    public LeaveAbsence toLeaveAbsence(UUID doctorId, CreateLeaveRequestDto requestDto, LocalDateTime startDateTime,
            LocalDateTime endDateTime) {
        LeaveAbsence leaveAbsence = new LeaveAbsence();
        leaveAbsence.setDoctorId(doctorId);
        leaveAbsence.setLeaveType(requestDto.getLeaveType());
        leaveAbsence.setStartDateTime(startDateTime);
        leaveAbsence.setEndDateTime(endDateTime);
        leaveAbsence.setStatus(LeaveStatus.PENDING);
        return leaveAbsence;
    }

    public LeaveResponseDto toLeaveResponseDto(LeaveAbsence leaveAbsence) {
        LeaveResponseDto dto = new LeaveResponseDto();
        dto.setId(leaveAbsence.getId());
        dto.setDoctorId(leaveAbsence.getDoctorId());
        dto.setLeaveType(leaveAbsence.getLeaveType());
        dto.setStartDateTime(leaveAbsence.getStartDateTime().format(DATE_TIME_FORMATTER));
        dto.setEndDateTime(leaveAbsence.getEndDateTime().format(DATE_TIME_FORMATTER));
        dto.setStatus(leaveAbsence.getStatus());
        return dto;
    }

    public DoctorAvailabilitySummaryDto toDoctorAvailabilitySummaryDto(Doctor doctor, boolean available) {
        DoctorAvailabilitySummaryDto dto = new DoctorAvailabilitySummaryDto();
        dto.setDoctorId(doctor.getId());
        dto.setName(doctor.getName());
        dto.setSpecialization(doctor.getSpecialization());
        dto.setAvailable(available);
        return dto;
    }
}
