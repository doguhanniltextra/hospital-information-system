package com.project.admission_service.readmodel;

import com.project.admission_service.model.AdmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdmissionSummaryRepository extends JpaRepository<AdmissionSummary, UUID> {
    List<AdmissionSummary> findByStatus(AdmissionStatus status);
}
