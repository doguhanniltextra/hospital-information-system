package com.project.patient_service.repository;

import com.project.patient_service.model.Patient;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);
    Patient findByEmail(String email);

    Optional<Patient> findByAuthUserId(UUID authUserId);
}

