package com.project.patient_service.readmodel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PatientSummaryRepository extends JpaRepository<PatientSummary, UUID> {
    boolean existsByEmail(String email);
}
