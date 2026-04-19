package com.project.appointment_service.repository;

import com.project.appointment_service.model.AppointmentSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AppointmentSummaryRepository extends JpaRepository<AppointmentSummary, UUID> {
    List<AppointmentSummary> findByPatientId(UUID patientId);
    List<AppointmentSummary> findByDoctorId(UUID doctorId);
}
