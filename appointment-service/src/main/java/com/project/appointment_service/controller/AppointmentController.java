package com.project.appointment_service.controller;

import com.project.appointment_service.constants.Endpoints;
import com.project.appointment_service.dto.AppointmentResponseDTO;
import com.project.appointment_service.dto.AppointmentUpdateDtoResponse;
import com.project.appointment_service.dto.DoctorAvailabilityPageResponseDTO;
import com.project.appointment_service.dto.request.CreateAppointmentServiceRequestDto;
import com.project.appointment_service.dto.response.AppointmentSummaryDto;
import com.project.appointment_service.dto.response.CreateAppointmentServiceResponseDto;
import com.project.appointment_service.helper.AppointmentMapper;
import com.project.appointment_service.helper.AppointmentSummaryMapper;
import com.project.appointment_service.model.ServiceType;
import com.project.appointment_service.utils.IdValidation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import com.project.appointment_service.dto.AppointmentDTO;
import com.project.appointment_service.model.Appointment;
import com.project.appointment_service.service.AppointmentService;

import java.util.UUID;

import org.springframework.http.ResponseEntity;

/**
 * REST Controller for managing Appointments.
 * Provides endpoints for creating, updating, deleting, and querying appointments,
 * as well as validating clinical entity existence and checking doctor availability.
 */
@RestController()
@RequestMapping(Endpoints.APPOINTMENT_CONTROLLER_REQUEST)
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final IdValidation idValidation;
    private final AppointmentResponseDTO appointmentResponseDTO;
    private final AppointmentMapper appointmentMapper;

    /**
     * Initializes the AppointmentController with necessary service components.
     * 
     * @param appointmentService Core service for appointment business logic
     * @param idValidation Service for validating IDs and checking availability via gRPC
     * @param appointmentResponseDTO DTO for transforming appointment results
     * @param appointmentMapper Mapper for mapping entities to DTOs
     */
    public AppointmentController(AppointmentService appointmentService, IdValidation idValidation, AppointmentResponseDTO appointmentResponseDTO, AppointmentMapper appointmentMapper) {
        this.appointmentService = appointmentService;
        this.idValidation = idValidation;
        this.appointmentResponseDTO = appointmentResponseDTO;
        this.appointmentMapper = appointmentMapper;
    }

    /**
     * Creates a new appointment.
     * Accessible by Patients (for their own), Receptionists, and Admins.
     * 
     * @param appointment The appointment entity details
     * @return The created appointment DTO
     */
    @PostMapping(Endpoints.CREATE_APPOINTMENT)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('PATIENT', 'RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<AppointmentDTO> createAppointment(@RequestBody Appointment appointment) {
        CreateAppointmentServiceRequestDto requestDto = appointmentMapper.getCreateAppointmentServiceRequestDto(appointment);
        CreateAppointmentServiceResponseDto responseDto = appointmentService.createAppointment(requestDto);
        AppointmentDTO appointmentDTO = appointmentResponseDTO.toDTO(responseDto);

        return ResponseEntity.ok(appointmentDTO);
    }


    /**
     * Updates an existing appointment's details.
     * Restricted to Admins, Receptionists, or the specific Patient who owns the appointment.
     * 
     * @param id The UUID of the appointment to update
     * @param appointment The updated appointment details
     * @return The updated appointment DTO
     */
    @PutMapping(Endpoints.UPDATE_APPOINTMENT)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST') or @securityService.isAppointmentOwner(authentication, #id)")
    public ResponseEntity<AppointmentDTO> updateAppointment(@PathVariable UUID id, @RequestBody Appointment appointment) {
        appointment.setId(id);
        Appointment updatedAppointment = appointmentService.updateAppointment(appointment);
        AppointmentDTO appointmentDTO = appointmentResponseDTO.toUpdateResponseDTO(updatedAppointment);
        return ResponseEntity.ok(appointmentDTO);
    }

    /**
     * Updates the payment/fulfillment status of an appointment.
     * Typically called after successful payment or service delivery.
     * 
     * @param id The UUID of the appointment
     * @param status The new status (true for paid/completed)
     * @return Success message DTO
     */
    @PutMapping(Endpoints.UPDATE_APPOINTMENT_STATUS)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<AppointmentUpdateDtoResponse> UpdateAppointmentStatus(@PathVariable UUID id, @PathVariable boolean status) {
        appointmentService.updatePaymentStatus(id, status);
        AppointmentUpdateDtoResponse appointmentUpdateDtoResponse = new AppointmentUpdateDtoResponse();
        appointmentUpdateDtoResponse.setMessage("Status Updated");
        return ResponseEntity.ok(appointmentUpdateDtoResponse);
    }

    /**
     * Deletes an appointment. Restricted to system administrators.
     * 
     * @param id The UUID of the appointment to delete
     * @return HTTP 204 No Content on success
     */
    @DeleteMapping(Endpoints.DELETE_APPOINTMENT)
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAppointment(@PathVariable UUID id) {
        appointmentService.deleteAppointment(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves a paginated list of all appointments.
     * 
     * @param page Page number (0-indexed)
     * @param size Page size (max 100)
     * @return Paginated list of appointments
     */
    @GetMapping(Endpoints.GET_ALL_APPOINTMENTS)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<Page<Appointment>> getAllAppointments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, safeSize);
        Page<Appointment> appointments = appointmentService.getAllAppointments(pageable);
        if (appointments.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(appointments);
    }

    /**
     * Retrieves a paginated list of appointment summaries including clinical details.
     * 
     * @param page Page number (0-indexed)
     * @param size Page size (max 100)
     * @return Paginated list of appointment summaries
     */
    @GetMapping(Endpoints.GET_ALL_APPOINTMENT_SUMMARIES)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<Page<AppointmentSummaryDto>> getAllAppointmentSummaries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, safeSize);
        Page<AppointmentSummaryDto> summaries = appointmentService.getAllAppointmentSummaries(pageable);
        if (summaries.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(summaries);
    }

    /**
     * Debug/Helper endpoint to validate patient and doctor existence via gRPC.
     * 
     * @param patientId UUID of the patient
     * @param doctorId UUID of the doctor
     * @return String result indicating existence of both entities
     */
    @GetMapping(Endpoints.VALIDATE_IDS)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public String validateIds(@PathVariable UUID patientId, @PathVariable UUID doctorId) {
        UUID patientIdResult = patientId;
        UUID doctorIdResult = doctorId;

        boolean patientExists = idValidation.checkPatientExists(patientId);
        boolean doctorExists = idValidation.checkDoctorExists(doctorId);

        return "Patient exists: " + patientExists + ", Doctor exists: " + doctorExists;
    }

    /**
     * Queries available doctors for a specific time range and service type.
     * 
     * @param start Start ISO date-time string
     * @param end End ISO date-time string
     * @param serviceType Type of clinical service
     * @param specialization Optional doctor specialization filter
     * @param page Page number
     * @param size Page size
     * @return Paginated list of available doctors and their time slots
     */
    @GetMapping(Endpoints.DOCTOR_OPTIONS)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('PATIENT', 'RECEPTIONIST', 'ADMIN', 'DOCTOR')")
    public ResponseEntity<DoctorAvailabilityPageResponseDTO> getDoctorOptions(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam ServiceType serviceType,
            @RequestParam(required = false) String specialization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        DoctorAvailabilityPageResponseDTO response = idValidation.getAvailableDoctorOptions(
                start, end, serviceType, specialization, page, Math.min(size, 100)
        );
        return ResponseEntity.ok(response);
    }
}
