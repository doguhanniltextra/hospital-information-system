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
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    @Transactional
    public void processPaymentUpdate(AppointmentDTO appointment) {
        BigDecimal totalAmount = BigDecimal.valueOf(appointment.getAmount());
        InsuranceStrategy insuranceStrategy = insuranceFactory.getStrategy(appointment.getInsuranceProviderType(), appointment.getProviderName());
        InsuranceCalculationResult split = insuranceStrategy.calculate(totalAmount);

        UUID invoiceId = UUID.randomUUID();
        String invoiceNumber = invoiceId.toString();
        String invoicePdfPath = invoiceService.generateInvoice(
                "Dr. " + appointment.getDoctorId(),
                "Patient " + appointment.getPatientId(),
                totalAmount,
                invoiceNumber
        ).toAbsolutePath().toString();

        Invoice invoice = new Invoice();
        invoice.setInvoiceId(invoiceId);
        invoice.setDoctorId(UUID.fromString(appointment.getDoctorId()));
        invoice.setPatientId(UUID.fromString(appointment.getPatientId()));
        invoice.setTotalAmount(totalAmount);
        invoice.setPatientOwes(split.getPatientOwes());
        invoice.setInsuranceOwes(split.getInsuranceOwes());
        invoice.setInvoicePdfUrl(invoicePdfPath);
        invoiceRepository.save(invoice);

        // Publish Application Event
        eventPublisher.publishEvent(new InvoiceGeneratedEvent(invoice));

        // Transactional Outbox
        saveOutboxEvent(invoiceId, "INVOICE", "INVOICE_GENERATED", invoice);

        if (split.getInsuranceOwes().compareTo(BigDecimal.ZERO) > 0) {
            submitClaim(invoice, appointment.getProviderName(), split.getInsuranceOwes());
        }
    }

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
        
        String invoicePdfPath = invoiceService.generateInvoice(
                "Hospital Facility",
                "Patient " + patientId,
                totalCharges,
                invoiceNumber
        ).toAbsolutePath().toString();

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

        // Transactional Outbox
        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("invoice", invoice);
        eventPayload.put("admissionId", admissionId);
        saveOutboxEvent(invoiceId, "DISCHARGE_BILLING", "DISCHARGE_BILLING_FINALIZED", eventPayload);
    }

    private void saveOutboxEvent(UUID aggregateId, String aggregateType, String eventType, Object payloadObj) {
        try {
            String payload = objectMapper.writeValueAsString(payloadObj);
            BillingOutboxEvent outboxEvent = new BillingOutboxEvent();
            outboxEvent.setAggregateId(aggregateId.toString());
            outboxEvent.setAggregateType(aggregateType);
            outboxEvent.setEventType(eventType);
            outboxEvent.setPayload(payload);
            outboxRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payload for outbox", e);
        }
    }

    private void submitClaim(Invoice invoice, String providerName, BigDecimal insuranceAmount) {
        ClaimRequestDto claimRequest = new ClaimRequestDto();
        claimRequest.setInvoiceId(invoice.getInvoiceId());
        claimRequest.setProviderName(providerName);
        claimRequest.setAmount(insuranceAmount);

        try {
            claimClient.submitClaim(claimRequest);
            Claim claim = new Claim();
            claim.setClaimId(UUID.randomUUID());
            claim.setInvoiceId(invoice.getInvoiceId());
            claim.setAmount(insuranceAmount);
            claim.setStatus(ClaimStatus.SUBMITTED);
            claimRepository.save(claim);
        } catch (Exception e) {
            log.error("Failed to submit claim for invoice {}", invoice.getInvoiceId(), e);
            Claim claim = new Claim();
            claim.setClaimId(UUID.randomUUID());
            claim.setInvoiceId(invoice.getInvoiceId());
            claim.setAmount(insuranceAmount);
            claim.setStatus(ClaimStatus.FAILED);
            claimRepository.save(claim);
        }
    }

    @Scheduled(fixedDelayString = "${claims.retry.fixed-delay-ms:300000}")
    public void retryFailedAndPendingClaims() {
        log.info("Retrying failed and pending claims...");
        List<Claim> retryableClaims = claimRepository.findByStatus(ClaimStatus.FAILED);
        // Also include pending if needed, but let's stick to failed for now as per previous command service
        for (Claim claim : retryableClaims) {
            try {
                ClaimRequestDto claimRequest = new ClaimRequestDto();
                claimRequest.setInvoiceId(claim.getInvoiceId());
                claimRequest.setProviderName("unknown"); // Note: provider name should ideally be stored in Claim model
                claimRequest.setAmount(claim.getAmount());

                claimClient.submitClaim(claimRequest);
                claim.setStatus(ClaimStatus.SUBMITTED);
                claimRepository.save(claim);
            } catch (Exception e) {
                log.error("Retry failed for claim {}", claim.getClaimId(), e);
            }
        }
    }

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
}