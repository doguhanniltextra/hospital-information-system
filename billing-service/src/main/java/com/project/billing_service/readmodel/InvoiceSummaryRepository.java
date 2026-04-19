package com.project.billing_service.readmodel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceSummaryRepository extends JpaRepository<InvoiceSummary, UUID> {

    List<InvoiceSummary> findByPatientId(UUID patientId);

    List<InvoiceSummary> findByStatus(String status);
}