package com.project.admission_service.repository;

import com.project.admission_service.model.AdmissionProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdmissionProcessedEventRepository extends JpaRepository<AdmissionProcessedEvent, String> {
    boolean existsByMessageId(String messageId);
}
