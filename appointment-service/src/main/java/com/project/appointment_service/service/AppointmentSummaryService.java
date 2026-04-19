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

@Service
public class AppointmentSummaryService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentSummaryService.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AppointmentSummaryRepository appointmentSummaryRepository;
    private final IdValidation idValidation;

    public AppointmentSummaryService(AppointmentSummaryRepository appointmentSummaryRepository,
                                     IdValidation idValidation) {
        this.appointmentSummaryRepository = appointmentSummaryRepository;
        this.idValidation = idValidation;
    }

    public void createOrUpdateSummary(Appointment appointment) {
        createOrUpdateSummary(appointment, null, null);
    }

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

    public void updatePaymentStatus(UUID appointmentId, boolean status) {
        appointmentSummaryRepository.findById(appointmentId).ifPresent(summary -> {
            summary.setStatus(status ? "PAID" : "PENDING");
            appointmentSummaryRepository.save(summary);
        });
    }

    public void deleteSummary(UUID appointmentId) {
        appointmentSummaryRepository.findById(appointmentId).ifPresent(summary -> appointmentSummaryRepository.delete(summary));
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<AppointmentSummaryDto> getAllAppointmentSummaries(Pageable pageable) {
        Page<AppointmentSummary> summaries = appointmentSummaryRepository.findAll(pageable);
        List<AppointmentSummaryDto> dtos = summaries.getContent().stream()
                .map(AppointmentSummaryMapper::toDto)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, summaries.getTotalElements());
    }
}
