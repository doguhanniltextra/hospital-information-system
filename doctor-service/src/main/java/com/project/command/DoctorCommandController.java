package com.project.command;

import com.project.constants.Endpoints;
import com.project.constants.SwaggerMessages;
import com.project.dto.UpdateDoctorControllerRequestDto;
import com.project.dto.UpdateDoctorControllerResponseDto;
import com.project.dto.UpdateDoctorServiceRequestDto;
import com.project.dto.UpdateDoctorServiceResponseDto;
import com.project.dto.request.CreateDoctorControllerRequestDto;
import com.project.dto.request.CreateDoctorServiceRequestDto;
import com.project.dto.request.CreateLeaveRequestDto;
import com.project.dto.request.CreateShiftRequestDto;
import com.project.dto.response.CreateDoctorControllerResponseDto;
import com.project.dto.response.CreateDoctorServiceResponseDto;
import com.project.dto.response.LeaveResponseDto;
import com.project.dto.response.ShiftResponseDto;
import com.project.exception.DoctorNotFoundException;
import com.project.exception.EmailIsNotUniqueException;
import com.project.exception.IdIsValidException.IdIsValidException;
import com.project.exception.PatientLimitException;
import com.project.helper.DoctorMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(Endpoints.DOCTOR_CONTROLLER_REQUEST)
@Tag(name = "Doctor Command Controller", description = "Mutation operations for Doctors")
public class DoctorCommandController {

    private final DoctorCommandService doctorCommandService;
    private final DoctorMapper doctorMapper;

    public DoctorCommandController(DoctorCommandService doctorCommandService, DoctorMapper doctorMapper) {
        this.doctorCommandService = doctorCommandService;
        this.doctorMapper = doctorMapper;
    }

    @PostMapping
    @Operation(summary = SwaggerMessages.CREATE_DOCTOR)
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CreateDoctorControllerResponseDto> createDoctor(@Valid @RequestBody CreateDoctorControllerRequestDto requestDto) 
            throws IdIsValidException, EmailIsNotUniqueException {
        
        CreateDoctorServiceRequestDto serviceRequestDto = doctorMapper.getCreateDoctorServiceRequestDto(requestDto);
        CreateDoctorCommand command = new CreateDoctorCommand(serviceRequestDto);
        
        CreateDoctorServiceResponseDto createdDoctor = doctorCommandService.createDoctor(command);
        return ResponseEntity.ok().body(toCreateControllerResponseDto(createdDoctor));
    }

    @PutMapping(Endpoints.DOCTOR_CONTROLLER_UPDATE_DOCTOR)
    @Operation(summary = SwaggerMessages.UPDATE_DOCTOR)
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or @securityService.isDoctorOwner(authentication, #id)")
    public ResponseEntity<UpdateDoctorControllerResponseDto> updateDoctor(@PathVariable UUID id, 
            @Validated({Default.class}) @RequestBody UpdateDoctorControllerRequestDto requestDto) throws DoctorNotFoundException {

        UpdateDoctorServiceRequestDto serviceRequestDto = new UpdateDoctorServiceRequestDto();
        serviceRequestDto.setEmail(requestDto.getEmail());
        serviceRequestDto.setName(requestDto.getName());
        serviceRequestDto.setHospitalName(requestDto.getHospitalName());
        serviceRequestDto.setSpecialization(requestDto.getSpecialization());
        serviceRequestDto.setLicenseNumber(requestDto.getLicenseNumber());
        serviceRequestDto.setMaxPatientCount(requestDto.getMaxPatientCount());

        UpdateDoctorCommand command = new UpdateDoctorCommand(id, serviceRequestDto);
        UpdateDoctorServiceResponseDto updatedDoctor = doctorCommandService.updateDoctor(command);

        return ResponseEntity.ok().body(DoctorMapper.getDoctorControllerResponseDto(updatedDoctor));
    }

    @DeleteMapping(Endpoints.DOCTOR_CONTROLLER_DELETE_DOCTOR)
    @Operation(summary = SwaggerMessages.DELETE_DOCTOR)
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDoctor(@PathVariable UUID id) {
        doctorCommandService.deleteDoctor(new DeleteDoctorCommand(id));
        return ResponseEntity.noContent().build();
    }

    @PutMapping(Endpoints.DOCTOR_CONTROLLER_INCREASE_PATIENT)
    @Operation(summary = "Increase patient count for a doctor")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('PATIENT', 'RECEPTIONIST', 'ADMIN', 'INTERNAL_SERVICE')")
    public ResponseEntity<Void> increasePatientNumber(@PathVariable UUID id) throws PatientLimitException, DoctorNotFoundException {
        doctorCommandService.increasePatientNumber(new IncreasePatientNumberCommand(id));
        return ResponseEntity.ok().build();
    }

    @PostMapping(Endpoints.DOCTOR_CONTROLLER_SHIFTS)
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or @securityService.isDoctorOwner(authentication, #doctorId)")
    public ResponseEntity<ShiftResponseDto> createShift(@PathVariable UUID doctorId, @Valid @RequestBody CreateShiftRequestDto requestDto) {
        return ResponseEntity.ok(doctorCommandService.createShift(new CreateShiftCommand(doctorId, requestDto)));
    }

    @DeleteMapping(Endpoints.DOCTOR_CONTROLLER_SHIFT_BY_ID)
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or @securityService.isDoctorOwner(authentication, #doctorId)")
    public ResponseEntity<Void> deleteShift(@PathVariable UUID doctorId, @PathVariable UUID shiftId) {
        doctorCommandService.deleteShift(new DeleteShiftCommand(doctorId, shiftId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping(Endpoints.DOCTOR_CONTROLLER_LEAVES)
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or @securityService.isDoctorOwner(authentication, #doctorId)")
    public ResponseEntity<LeaveResponseDto> createLeave(@PathVariable UUID doctorId, @Valid @RequestBody CreateLeaveRequestDto requestDto) {
        return ResponseEntity.ok(doctorCommandService.createLeave(new CreateLeaveCommand(doctorId, requestDto)));
    }

    @PutMapping(Endpoints.DOCTOR_CONTROLLER_LEAVE_APPROVE)
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LeaveResponseDto> approveLeave(@PathVariable UUID doctorId, @PathVariable UUID leaveId) {
        return ResponseEntity.ok(doctorCommandService.approveLeave(new ApproveLeaveCommand(doctorId, leaveId)));
    }

    @DeleteMapping(Endpoints.DOCTOR_CONTROLLER_LEAVE_BY_ID)
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN') or @securityService.isOwnPendingLeave(authentication, #doctorId, #leaveId)")
    public ResponseEntity<Void> deleteLeave(@PathVariable UUID doctorId, @PathVariable UUID leaveId) {
        doctorCommandService.deleteLeave(new DeleteLeaveCommand(doctorId, leaveId));
        return ResponseEntity.noContent().build();
    }

    private CreateDoctorControllerResponseDto toCreateControllerResponseDto(CreateDoctorServiceResponseDto createdDoctor) {
        CreateDoctorControllerResponseDto responseDto = new CreateDoctorControllerResponseDto();
        responseDto.setId(createdDoctor.getId());
        responseDto.setName(createdDoctor.getName());
        responseDto.setEmail(createdDoctor.getEmail());
        responseDto.setNumber(createdDoctor.getNumber());
        responseDto.setHospitalName(createdDoctor.getHospitalName());
        return responseDto;
    }
}
