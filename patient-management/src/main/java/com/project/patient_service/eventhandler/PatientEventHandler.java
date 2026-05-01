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

/**
 * Component responsible for maintaining the CQRS Read Model (PatientSummary).
 * Listens for application events and synchronizes changes to the denormalized store.
 */
@Component
public class PatientEventHandler {

    private static final Logger log = LoggerFactory.getLogger(PatientEventHandler.class);
    private final PatientSummaryRepository patientSummaryRepository;

    /**
     * Initializes the event handler with the summary repository.
     * 
     * @param patientSummaryRepository Repository for the read model
     */
    public PatientEventHandler(PatientSummaryRepository patientSummaryRepository) {
        this.patientSummaryRepository = patientSummaryRepository;
    }

    /**
     * Handles the creation of a new patient.
     * Triggered after the database transaction is successfully committed.
     * 
     * @param event The event containing the newly created patient entity
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePatientCreated(PatientCreatedEvent event) {
        log.info("Synchronizing new patient to Read Model: {}", event.patient().getId());
        upsertSummary(event.patient());
    }

    /**
     * Handles updates to an existing patient record.
     * Triggered after the database transaction is successfully committed.
     * 
     * @param event The event containing the updated patient entity
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePatientUpdated(PatientUpdatedEvent event) {
        log.info("Synchronizing updated patient to Read Model: {}", event.patient().getId());
        upsertSummary(event.patient());
    }

    /**
     * Handles the deletion of a patient record.
     * Removes the corresponding entry from the read model.
     * 
     * @param event The event containing the deleted patient's identifier
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePatientDeleted(PatientDeletedEvent event) {
        log.info("Removing patient from Read Model: {}", event.id());
        patientSummaryRepository.deleteById(event.id());
    }

    /**
     * Internal helper to create or update a patient summary record based on a domain entity.
     * 
     * @param patient The clinical patient entity to synchronize
     */
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

