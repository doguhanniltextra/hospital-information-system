package com.project.billing_service.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.billing_service.client.ClaimClient;
import com.project.billing_service.dto.AppointmentDTO;
import com.project.billing_service.dto.ClaimRequestDto;
import com.project.billing_service.event.InvoiceGeneratedEvent;
import com.project.billing_service.model.*;
import com.project.billing_service.repository.BillingOutboxRepository;
import com.project.billing_service.repository.ClaimRepository;
import com.project.billing_service.repository.InvoiceRepository;
import com.project.billing_service.repository.UnbilledChargeRepository;
import com.project.billing_service.strategy.InsuranceCalculationResult;
import com.project.billing_service.strategy.InsuranceFactory;
import com.project.billing_service.strategy.InsuranceStrategy;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service class responsible for processing financial commands in the billing microservice.
 * Handles payment updates from appointments, finalizes discharge billing, 
 * manages unbilled charges, and coordinates with insurance claim processing.
 * 
 * Implements the Transactional Outbox pattern for reliable event propagation.
 */
@Service
public class BillingCommandService {
    private static final Logger log = LoggerFactory.getLogger(BillingCommandService.class);

    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;
    private final ClaimRepository claimRepository;
    private final UnbilledChargeRepository unbilledChargeRepository;
    private final BillingOutboxRepository outboxRepository;
    private final InsuranceFactory insuranceFactory;
    private final ClaimClient claimClient;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Initializes the billing command service with required dependencies.
     * 
     * @param invoiceService Service for generating PDF invoices
     * @param invoiceRepository Repository for invoice persistence
     * @param claimRepository Repository for insurance claim persistence
     * @param unbilledChargeRepository Repository for tracking unbilled services
     * @param outboxRepository Repository for Transactional Outbox events
     * @param insuranceFactory Factory for retrieving insurance calculation strategies
     * @param claimClient External client for submitting claims
     * @param objectMapper Mapper for JSON serialization
     * @param eventPublisher Publisher for internal application events
     */
    public BillingCommandService(
            InvoiceService invoiceService,
            InvoiceRepository invoiceRepository,
            ClaimRepository claimRepository,
            UnbilledChargeRepository unbilledChargeRepository,
            BillingOutboxRepository outboxRepository,
            InsuranceFactory insuranceFactory,
            ClaimClient claimClient,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher) {
        this.invoiceService = invoiceService;
        this.invoiceRepository = invoiceRepository;
        this.claimRepository = claimRepository;
        this.unbilledChargeRepository = unbilledChargeRepository;
        this.outboxRepository = outboxRepository;
        this.insuranceFactory = insuranceFactory;
        this.claimClient = claimClient;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Processes a payment update for a completed appointment.
     * Calculates the insurance split, generates an invoice, and submits an insurance claim if applicable.
     * 
     * @param appointment DTO containing appointment and insurance details
     */
    @Transactional
    public void processPaymentUpdate(AppointmentDTO appointment) {
        BigDecimal totalAmount = BigDecimal.valueOf(appointment.getAmount());
        InsuranceStrategy insuranceStrategy = insuranceFactory.getStrategy(
                appointment.getInsuranceProviderType(), appointment.getProviderName());
        InsuranceCalculationResult split = insuranceStrategy.calculate(totalAmount);

        UUID invoiceId = UUID.randomUUID();
        String invoiceNumber = invoiceId.toString();

        // FIX: PDF generation via external API is best-effort.
        // If the third-party service is down we must NOT roll back the financial record.
        // The invoice is created without a PDF URL; a separate retry job can regenerate it.
        String invoicePdfPath = null;
        try {
            invoicePdfPath = invoiceService.generateInvoice(
                    "Dr. " + appointment.getDoctorId(),
                    "Patient " + appointment.getPatientId(),
                    totalAmount,
                    invoiceNumber
            ).toAbsolutePath().toString();
        } catch (Exception e) {
            log.error("Failed to generate invoice PDF for invoice {}. Invoice will be persisted without a PDF URL.", invoiceId, e);
        }

        Invoice invoice = new Invoice();
        invoice.setInvoiceId(invoiceId);
        invoice.setDoctorId(UUID.fromString(appointment.getDoctorId()));
        invoice.setPatientId(UUID.fromString(appointment.getPatientId()));
        invoice.setTotalAmount(totalAmount);
        invoice.setPatientOwes(split.getPatientOwes());
        invoice.setInsuranceOwes(split.getInsuranceOwes());
        invoice.setInvoicePdfUrl(invoicePdfPath);
        invoiceRepository.save(invoice);

        // Publish Application Event (syncs read model in same TX via AFTER_COMMIT listener)
        eventPublisher.publishEvent(new InvoiceGeneratedEvent(invoice));

        // FIX: saveOutboxEvent now throws on serialization failure → triggers TX rollback
        saveOutboxEvent(invoiceId, "INVOICE", "INVOICE_GENERATED", invoice);

        if (split.getInsuranceOwes().compareTo(BigDecimal.ZERO) > 0) {
            submitClaim(invoice, appointment.getProviderName(), split.getInsuranceOwes());
        }
    }

    /**
     * Finalizes billing for a patient being discharged.
     * Consolidates all open charges (Lab, Inventory, Bed) into a final invoice.
     * 
     * @param patientId The unique ID of the patient
     * @param admissionId The unique ID of the admission
     * @param doctorId The unique ID of the primary doctor
     */
    @Transactional
    public void finalizeDischargeBilling(UUID patientId, UUID admissionId, UUID doctorId) {
        List<UnbilledCharge> openCharges = unbilledChargeRepository.findByPatientIdAndStatus(patientId, "OPEN");

        if (openCharges.isEmpty()) {
            log.info("No open charges found for patient {} at discharge.", patientId);
            return;
        }

        BigDecimal totalCharges = openCharges.stream()
                .map(UnbilledCharge::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        UUID invoiceId = UUID.randomUUID();
        String invoiceNumber = "IP-" + admissionId.toString().substring(0, 8);

        // NOTE: Discharge billing currently assumes the patient bears the full amount
        // because the discharge Kafka event does not carry insurance information.
        // When the admission service is extended to include insurance fields in the
        // `admission-discharged.v1` payload, wire InsuranceFactory here.
        log.warn("Discharge billing for patient {} uses no insurance split. " +
                "Ensure admission-discharged event includes insurance info.", patientId);

        String invoicePdfPath = null;
        try {
            invoicePdfPath = invoiceService.generateInvoice(
                    "Hospital Facility",
                    "Patient " + patientId,
                    totalCharges,
                    invoiceNumber
            ).toAbsolutePath().toString();
        } catch (Exception e) {
            log.error("Failed to generate discharge invoice PDF for invoice {}. Invoice will be persisted without a PDF URL.", invoiceId, e);
        }

        Invoice invoice = new Invoice();
        invoice.setInvoiceId(invoiceId);
        invoice.setPatientId(patientId);
        invoice.setDoctorId(doctorId);
        invoice.setTotalAmount(totalCharges);
        invoice.setPatientOwes(totalCharges);
        invoice.setInsuranceOwes(BigDecimal.ZERO);
        invoice.setInvoicePdfUrl(invoicePdfPath);
        invoiceRepository.save(invoice);

        eventPublisher.publishEvent(new InvoiceGeneratedEvent(invoice));

        // Mark charges as billed
        openCharges.forEach(charge -> charge.setStatus("BILLED"));
        unbilledChargeRepository.saveAll(openCharges);

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("invoice", invoice);
        eventPayload.put("admissionId", admissionId);
        saveOutboxEvent(invoiceId, "DISCHARGE_BILLING", "DISCHARGE_BILLING_FINALIZED", eventPayload);
    }

    /**
     * Persists an event to the outbox table for reliable Kafka publishing.
     * Throws a RuntimeException if serialization fails to ensure transaction rollback.
     */
    private void saveOutboxEvent(UUID aggregateId, String aggregateType, String eventType, Object payloadObj) {
        try {
            String payload = objectMapper.writeValueAsString(payloadObj);
            BillingOutboxEvent outboxEvent = new BillingOutboxEvent(
                    aggregateId.toString(), aggregateType, eventType, payload);
            outboxRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "Failed to serialize outbox event payload for aggregate " + aggregateId + ". Rolling back transaction.", e);
        }
    }

    /**
     * Scheduled job to retry failed or pending insurance claims.
     * Uses stored provider context to re-submit claims to external systems.
     */
    @Scheduled(fixedDelayString = "${claims.retry.fixed-delay-ms:300000}")
    public void retryFailedAndPendingClaims() {
        log.info("Retrying failed claims...");
        List<Claim> retryableClaims = claimRepository.findByStatus(ClaimStatus.FAILED);

        for (Claim claim : retryableClaims) {
            if (claim.getProviderName() == null || claim.getProviderName().isBlank()) {
                log.warn("Claim {} has no providerName stored — cannot retry. Skipping.", claim.getClaimId());
                continue;
            }
            try {
                ClaimRequestDto claimRequest = new ClaimRequestDto();
                claimRequest.setInvoiceId(claim.getInvoiceId());
                claimRequest.setProviderName(claim.getProviderName()); // FIX: use stored value
                claimRequest.setAmount(claim.getAmount());

                claimClient.submitClaim(claimRequest);
                claim.setStatus(ClaimStatus.SUBMITTED);
                claimRepository.save(claim);
                log.info("Claim {} successfully resubmitted.", claim.getClaimId());
            } catch (Exception e) {
                log.error("Retry failed for claim {}", claim.getClaimId(), e);
            }
        }
    }

    /**
     * Records a new unbilled charge originating from a laboratory order.
     */
    @Transactional
    public void createUnbilledLabCharge(UUID patientId, UUID sourceOrderId, BigDecimal amount, String currency) {
        if (unbilledChargeRepository.findBySourceTypeAndSourceOrderId("LAB", sourceOrderId).isPresent()) {
            return;
        }
        UnbilledCharge charge = new UnbilledCharge();
        charge.setPatientId(patientId);
        charge.setSourceType("LAB");
        charge.setSourceOrderId(sourceOrderId);
        charge.setAmount(amount);
        charge.setCurrency(currency);
        charge.setStatus("OPEN");
        charge.setCreatedAt(Instant.now());
        unbilledChargeRepository.save(charge);
    }

    /**
     * Records a new unbilled charge originating from inventory usage.
     */
    @Transactional
    public void createUnbilledInventoryCharge(UUID patientId, UUID itemId, Integer quantity, BigDecimal unitPrice, String currency, UUID eventId) {
        if (unbilledChargeRepository.findBySourceTypeAndSourceOrderId("INVENTORY", eventId).isPresent()) {
            return;
        }
        UnbilledCharge charge = new UnbilledCharge();
        charge.setPatientId(patientId);
        charge.setSourceType("INVENTORY");
        charge.setSourceOrderId(eventId);
        charge.setAmount(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        charge.setCurrency(currency);
        charge.setStatus("OPEN");
        charge.setCreatedAt(Instant.now());
        unbilledChargeRepository.save(charge);
    }

    /**
     * Records a new unbilled charge for hospital bed occupancy.
     */
    @Transactional
    public void createUnbilledBedCharge(UUID patientId, UUID admissionId, BigDecimal amount, String currency, UUID eventId) {
        if (unbilledChargeRepository.findBySourceTypeAndSourceOrderId("BED", eventId).isPresent()) {
            return;
        }
        UnbilledCharge charge = new UnbilledCharge();
        charge.setPatientId(patientId);
        charge.setSourceType("BED");
        charge.setSourceOrderId(eventId);
        charge.setAmount(amount);
        charge.setCurrency(currency);
        charge.setStatus("OPEN");
        charge.setCreatedAt(Instant.now());
        unbilledChargeRepository.save(charge);
    }

    /**
     * Submits an insurance claim via the external ClaimClient.
     * Persists the claim record with its initial status (SUBMITTED or FAILED).
     */
    private void submitClaim(Invoice invoice, String providerName, BigDecimal insuranceAmount) {
        ClaimRequestDto claimRequest = new ClaimRequestDto();
        claimRequest.setInvoiceId(invoice.getInvoiceId());
        claimRequest.setProviderName(providerName);
        claimRequest.setAmount(insuranceAmount);

        Claim claim = new Claim();
        claim.setClaimId(UUID.randomUUID());
        claim.setInvoiceId(invoice.getInvoiceId());
        claim.setAmount(insuranceAmount);
        claim.setProviderName(providerName); // FIX: persist for retry
        claim.setSubmittedAt(LocalDateTime.now());

        try {
            claimClient.submitClaim(claimRequest);
            claim.setStatus(ClaimStatus.SUBMITTED);
        } catch (Exception e) {
            log.error("Failed to submit claim for invoice {}", invoice.getInvoiceId(), e);
            claim.setStatus(ClaimStatus.FAILED);
        }
        claimRepository.save(claim);
    }
}

}