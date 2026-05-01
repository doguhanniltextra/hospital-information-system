package com.project.patient_service.repository;

import com.project.patient_service.model.Patient;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for the Patient write model.
 * Handles persistence for the core clinical patient records.
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    
    /**
     * Checks if a patient exists with the given email.
     * 
     * @param email The email to check
     * @return True if exists
     */
    boolean existsByEmail(String email);

    /**
     * Checks if another patient record exists with the given email, excluding the specified ID.
     * 
     * @param email The email to check
     * @param id The ID to exclude
     * @return True if a conflict is found
     */
    boolean existsByEmailAndIdNot(String email, UUID id);

    /**
     * Finds a patient record by email.
     * 
     * @param email The email to search for
     * @return The patient record
     */
    Patient findByEmail(String email);

    /**
     * Finds a patient record by their linked auth-service user ID.
     * 
     * @param authUserId The external authentication identifier
     * @return Optional containing the patient if linked
     */
    Optional<Patient> findByAuthUserId(UUID authUserId);
}


