package com.project.query;

import com.project.constants.Endpoints;
import com.project.constants.SwaggerMessages;
import com.project.dto.response.AvailabilityResponseDto;
import com.project.dto.response.DoctorAvailabilitySummaryDto;
import com.project.dto.response.ShiftResponseDto;
import com.project.model.Doctor;
import com.project.model.ServiceType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(Endpoints.DOCTOR_CONTROLLER_REQUEST)
@Tag(name = "Doctor Query Controller", description = "Query operations for Doctors")
public class DoctorQueryController {

    private final DoctorQueryService doctorQueryService;

    public DoctorQueryController(DoctorQueryService doctorQueryService) {
        this.doctorQueryService = doctorQueryService;
    }

    @GetMapping
    @Operation(summary = SwaggerMessages.GET_DOCTORS)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<Page<Doctor>> getDoctors(
            @RequestParam(required = false) String specialization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, safeSize);
        GetDoctorsQuery query = new GetDoctorsQuery(pageable, specialization);
        Page<Doctor> doctors = doctorQueryService.getDoctors(query);
        return ResponseEntity.ok().body(doctors);
    }

    @GetMapping(Endpoints.DOCTOR_CONTROLLER_FIND_DOCTOR_BY_ID)
    @Operation(summary = SwaggerMessages.FIND_DOCTOR_BY_ID)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'INTERNAL_SERVICE') or @securityService.isDoctorOwner(authentication, #id)")
    public ResponseEntity<Doctor> findDoctorById(@PathVariable UUID id) {
        Optional<Doctor> currentId = doctorQueryService.findDoctorById(new GetDoctorQuery(id));
        return currentId.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping(Endpoints.DOCTOR_CONTROLLER_SHIFTS)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST') or @securityService.isDoctorOwner(authentication, #doctorId)")
    public ResponseEntity<List<ShiftResponseDto>> listShifts(
            @PathVariable UUID doctorId,
            @RequestParam String fromDate,
            @RequestParam String toDate
    ) {
        try {
            LocalDate from = LocalDate.parse(fromDate);
            LocalDate to = LocalDate.parse(toDate);
            return ResponseEntity.ok(doctorQueryService.listShifts(doctorId, from, to));
        } catch (DateTimeParseException ex) {
            throw new com.project.exception.ApiException("INVALID_SLOT", "Invalid date format, expected yyyy-MM-dd", 400);
        }
    }

    @GetMapping(Endpoints.DOCTOR_CONTROLLER_AVAILABILITY_BY_DOCTOR)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'PATIENT', 'DOCTOR', 'INTERNAL_SERVICE')")
    public ResponseEntity<AvailabilityResponseDto> checkAvailability(
            @PathVariable UUID doctorId,
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam ServiceType serviceType
    ) {
        GetAvailableDoctorsQuery query = new GetAvailableDoctorsQuery(start, end, serviceType);
        return ResponseEntity.ok(doctorQueryService.checkDoctorAvailability(doctorId, query));
    }

    @GetMapping(Endpoints.DOCTOR_CONTROLLER_AVAILABILITY)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'PATIENT', 'DOCTOR', 'INTERNAL_SERVICE')")
    public ResponseEntity<Page<DoctorAvailabilitySummaryDto>> getAvailableDoctors(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam ServiceType serviceType,
            @RequestParam(required = false) String specialization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        GetAvailableDoctorsQuery query = new GetAvailableDoctorsQuery(start, end, serviceType);
        return ResponseEntity.ok(doctorQueryService.getAvailableDoctors(query, specialization, pageable));
    }

}
