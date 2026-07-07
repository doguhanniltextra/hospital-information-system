package com.project.appointment_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * A dummy entity used solely for acquiring pessimistic write locks on a doctor's 
 * schedule within the appointment-service to prevent overlapping appointments.
 */
@Entity
@Table(name = "doctor_locks")
public class DoctorLock {

    @Id
    @Column(name = "doctor_id")
    private UUID doctorId;

    public DoctorLock() {
    }

    public DoctorLock(UUID doctorId) {
        this.doctorId = doctorId;
    }

    public UUID getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(UUID doctorId) {
        this.doctorId = doctorId;
    }
}
