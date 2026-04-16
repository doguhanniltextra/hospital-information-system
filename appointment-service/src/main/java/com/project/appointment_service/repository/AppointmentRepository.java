package com.project.appointment_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.appointment_service.model.Appointment;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findByPatientId(UUID patientId);
    List<Appointment> findByDoctorId(UUID doctorId);

    void deleteByPatientId(UUID patientId);
    void deleteByDoctorId(UUID doctorId);

    @Query("SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND ((a.serviceDate <= :end AND a.serviceDateEnd >= :start))")
    List<Appointment> findOverlappingAppointments(@Param("doctorId") UUID doctorId, @Param("start") String start, @Param("end") String end);

    /**
     * Deletes expired appointments that have been paid.
     * Compares serviceDateEnd (stored as 'yyyy-MM-dd HH:mm' string) against the cutoff.
     * Returns the number of deleted rows.
     */
    @Modifying
    @Query("DELETE FROM Appointment a WHERE a.paymentStatus = true AND a.serviceDateEnd < :cutoff")
    int deleteExpiredPaidAppointments(@Param("cutoff") String cutoff);
}
