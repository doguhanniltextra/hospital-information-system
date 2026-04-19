package com.project.patient_service.repository;

import com.project.patient_service.model.PatientOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PatientOutboxRepository extends JpaRepository<PatientOutboxEvent, UUID> {
    List<PatientOutboxEvent> findByStatusOrderByCreatedAtAsc(String status);
}
