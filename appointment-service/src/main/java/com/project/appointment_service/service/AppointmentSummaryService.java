package com.project.appointment_service.service;

import com.project.appointment_service.dto.DoctorInfoDTO;
import com.project.appointment_service.dto.PatientInfoDTO;
import com.project.appointment_service.dto.response.AppointmentSummaryDto;
import com.project.appointment_service.helper.AppointmentSummaryMapper;
import com.project.appointment_service.model.Appointment;
import com.project.appointment_service.model.AppointmentSummary;
import com.project.appointment_service.repository.AppointmentSummaryRepository;
import com.project.appointment_service.utils.IdValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing de-normalized Appointment Summaries.
 * These summaries combine appointment details with metadata from other services (Patient, Doctor)
 * to facilitate efficient read operations and reporting without cross-service joins.
 */
@Service
public class AppointmentSummaryService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentSummaryService.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AppointmentSummaryRepository appointmentSummaryRepository;
    private final IdValidation idValidation;

    /**
     * Initializes the AppointmentSummaryService.
     * 
     * @param appointmentSummaryRepository Repository for AppointmentSummary entities
     * @param idValidation Service for fetching metadata from external services
     */
    public AppointmentSummaryService(AppointmentSummaryRepository appointmentSummaryRepository,
                                     IdValidation idValidation) {
        this.appointmentSummaryRepository = appointmentSummaryRepository;
        this.idValidation = idValidation;
    }

    /**
     * Overloaded method to create or update a summary using internal gRPC calls for missing info.
     * 
     * @param appointment The appointment entity
     */
    public void createOrUpdateSummary(Appointment appointment) {
        createOrUpdateSummary(appointment, null, null);
    }

    /**
     * Core logic to create or update an appointment summary.
     * Populates clinical metadata (names, emails, specializations) by either:
     * 1. Using provided info (from a Saga execution).
     * 2. Fetching via gRPC (for manual updates or re-syncs).
     * 
     * @param appointment The source appointment entity
     * @param patientInfo Optional pre-fetched patient metadata
     * @param doctorInfo Optional pre-fetched doctor metadata
     */
    public void createOrUpdateSummary(Appointment appointment, PatientInfoDTO patientInfo, DoctorInfoDTO doctorInfo) {
        if (appointment == null || appointment.getId() == null) {
            log.warn("AppointmentSummaryService: appointment or appointment.id is null; skipping summary creation.");
            return;
        }

        AppointmentSummary summary = appointmentSummaryRepository.findById(appointment.getId())
                .orElseGet(AppointmentSummary::new);

        summary.setId(appointment.getId());
        summary.setPatientId(appointment.getPatientId());
        summary.setDoctorId(appointment.getDoctorId());
        
        try {
            summary.setAppointmentTime(LocalDateTime.parse(appointment.getServiceDate(), formatter));
        } catch (Exception e) {
            log.warn("Could not parse service date: {}, Error: {}", appointment.getServiceDate(), e.getMessage());
        }
        
        summary.setServiceType(appointment.getServiceType());
        summary.setAmount(appointment.getAmount());
        summary.setStatus(appointment.isPaymentStatus() ? "PAID" : "PENDING");

        try {
            PatientInfoDTO finalPatientInfo = (patientInfo != null) ? patientInfo : idValidation.fetchPatientInfo(appointment.getPatientId());
            if (finalPatientInfo != null) {
                summary.setPatientName(finalPatientInfo.getName());
                summary.setPatientEmail(finalPatientInfo.getEmail());
            }
        } catch (Exception ex) {
            log.warn("Unable to fetch patient details for appointment summary: {}", ex.getMessage());
        }

        try {
            DoctorInfoDTO finalDoctorInfo = (doctorInfo != null) ? doctorInfo : idValidation.fetchDoctorInfo(appointment.getDoctorId());
            if (finalDoctorInfo != null) {
                summary.setDoctorName(finalDoctorInfo.getName());
                summary.setDoctorSpecialization(finalDoctorInfo.getSpecialization());
            }
        } catch (Exception ex) {
            log.warn("Unable to fetch doctor details for appointment summary: {}", ex.getMessage());
        }

        appointmentSummaryRepository.save(summary);
        log.info("AppointmentSummary saved for appointment id={}", appointment.getId());
    }

    /**
     * Updates the payment status in the summary table.
     * 
     * @param appointmentId The UUID of the appointment
     * @param status The new payment status (true for PAID)
     */
    public void updatePaymentStatus(UUID appointmentId, boolean status) {
        appointmentSummaryRepository.findById(appointmentId).ifPresent(summary -> {
            summary.setStatus(status ? "PAID" : "PENDING");
            appointmentSummaryRepository.save(summary);
        });
    }

    /**
     * Deletes an appointment summary.
     * 
     * @param appointmentId The UUID of the appointment
     */
    public void deleteSummary(UUID appointmentId) {
        appointmentSummaryRepository.findById(appointmentId).ifPresent(summary -> appointmentSummaryRepository.delete(summary));
    }

    /**
     * Retrieves all appointment summaries in a paginated format.
     * 
     * @param pageable Pagination details
     * @return A page of appointment summary DTOs
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<AppointmentSummaryDto> getAllAppointmentSummaries(Pageable pageable) {
        Page<AppointmentSummary> summaries = appointmentSummaryRepository.findAll(pageable);
        List<AppointmentSummaryDto> dtos = summaries.getContent().stream()
                .map(AppointmentSummaryMapper::toDto)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, summaries.getTotalElements());
    }
}
