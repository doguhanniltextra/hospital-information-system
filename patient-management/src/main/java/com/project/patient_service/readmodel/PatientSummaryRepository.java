package com.project.patient_service.readmodel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for accessing the PatientSummary read model (CQRS).
 * This model is optimized for query operations and contains denormalized data.
 */
@Repository
public interface PatientSummaryRepository extends JpaRepository<PatientSummary, UUID> {
    
    /**
     * Checks if a patient record already exists with the given email.
     * 
     * @param email The email to check
     * @return True if a record exists
     */
    boolean existsByEmail(String email);

    /**
     * Finds a patient summary by their authentication system identifier.
     * 
     * @param authUserId The UUID from auth-service
     * @return Optional containing the summary if found
     */
    Optional<PatientSummary> findByAuthUserId(UUID authUserId);
}


