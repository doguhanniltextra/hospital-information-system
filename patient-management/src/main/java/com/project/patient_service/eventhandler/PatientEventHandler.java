package com.project.patient_service.eventhandler;

import com.project.patient_service.event.PatientCreatedEvent;
import com.project.patient_service.event.PatientDeletedEvent;
import com.project.patient_service.event.PatientUpdatedEvent;
import com.project.patient_service.model.Patient;
import com.project.patient_service.readmodel.PatientSummary;
import com.project.patient_service.readmodel.PatientSummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Component
public class PatientEventHandler {

    private static final Logger log = LoggerFactory.getLogger(PatientEventHandler.class);
    private final PatientSummaryRepository patientSummaryRepository;

    public PatientEventHandler(PatientSummaryRepository patientSummaryRepository) {
        this.patientSummaryRepository = patientSummaryRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePatientCreated(PatientCreatedEvent event) {
        log.info("Synchronizing new patient to Read Model: {}", event.patient().getId());
        upsertSummary(event.patient());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePatientUpdated(PatientUpdatedEvent event) {
        log.info("Synchronizing updated patient to Read Model: {}", event.patient().getId());
        upsertSummary(event.patient());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePatientDeleted(PatientDeletedEvent event) {
        log.info("Removing patient from Read Model: {}", event.id());
        patientSummaryRepository.deleteById(event.id());
    }

    private void upsertSummary(Patient patient) {
        PatientSummary summary = patientSummaryRepository.findById(patient.getId())
                .orElse(new PatientSummary());

        summary.setId(patient.getId());
        summary.setName(patient.getName());
        summary.setEmail(patient.getEmail());
        summary.setPhoneNumber(patient.getPhoneNumber());
        
        if (patient.getInsuranceInfo() != null) {
            summary.setInsuranceProviderName(patient.getInsuranceInfo().getProviderName());
            summary.setInsurancePolicyNumber(patient.getInsuranceInfo().getPolicyNumber());
        }

        summary.setLastUpdated(LocalDateTime.now());
        summary.setAuthUserId(patient.getAuthUserId());
        patientSummaryRepository.save(summary);
    }
}
