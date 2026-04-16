package com.project.billing_service.service;

import com.project.billing_service.client.ClaimClient;
import com.project.billing_service.dto.AppointmentDTO;
import com.project.billing_service.dto.ClaimRequestDto;
import com.project.billing_service.model.Claim;
import com.project.billing_service.model.ClaimStatus;
import com.project.billing_service.model.Invoice;
import com.project.billing_service.model.UnbilledCharge;
import com.project.billing_service.repository.ClaimRepository;
import com.project.billing_service.repository.InvoiceRepository;
import com.project.billing_service.repository.UnbilledChargeRepository;
import com.project.billing_service.strategy.InsuranceCalculationResult;
import com.project.billing_service.strategy.InsuranceFactory;
import com.project.billing_service.strategy.InsuranceStrategy;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BillingWorkflowService {
    private static final Logger log = LoggerFactory.getLogger(BillingWorkflowService.class);

    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;
    private final ClaimRepository claimRepository;
    private final UnbilledChargeRepository unbilledChargeRepository;
    private final InsuranceFactory insuranceFactory;
    private final ClaimClient claimClient;

    public BillingWorkflowService(
            InvoiceService invoiceService,
            InvoiceRepository invoiceRepository,
            ClaimRepository claimRepository,
            UnbilledChargeRepository unbilledChargeRepository,
            InsuranceFactory insuranceFactory,
            ClaimClient claimClient
    ) {
        this.invoiceService = invoiceService;
        this.invoiceRepository = invoiceRepository;
        this.claimRepository = claimRepository;
        this.unbilledChargeRepository = unbilledChargeRepository;
        this.insuranceFactory = insuranceFactory;
        this.claimClient = claimClient;
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
                split.getPatientOwes(),
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

        if (split.getInsuranceOwes().compareTo(BigDecimal.ZERO) > 0) {
            submitClaim(invoice, appointment.getProviderName(), split.getInsuranceOwes());
        }
    }

    @Transactional
    public void retryPendingClaims() {
        List<Claim> pendingClaims = claimRepository.findByStatus(ClaimStatus.PENDING);
        for (Claim pendingClaim : pendingClaims) {
            invoiceRepository.findById(pendingClaim.getInvoiceId()).ifPresent(invoice -> {
                boolean success = submitClaimToProvider(pendingClaim, invoice.getInsuranceOwes());
                if (success) {
                    pendingClaim.setStatus(ClaimStatus.APPROVED);
                    pendingClaim.setSubmittedAt(LocalDateTime.now());
                    claimRepository.save(pendingClaim);
                }
            });
        }
    }

    @Scheduled(fixedDelayString = "${claims.retry.fixed-delay-ms:300000}")
    public void retryPendingClaimsScheduled() {
        log.info("Retrying pending claims");
        retryPendingClaims();
    }

    private void submitClaim(Invoice invoice, String providerName, BigDecimal claimAmount) {
        Claim claim = new Claim();
        claim.setClaimId(UUID.randomUUID());
        claim.setInvoiceId(invoice.getInvoiceId());
        claim.setProviderName(providerName);
        claim.setStatus(ClaimStatus.PENDING);
        claim.setSubmittedAt(LocalDateTime.now());

        boolean success = submitClaimToProvider(claim, claimAmount);
        if (success) {
            claim.setStatus(ClaimStatus.APPROVED);
            claim.setSubmittedAt(LocalDateTime.now());
        }
        claimRepository.save(claim);
    }

    private boolean submitClaimToProvider(Claim claim, BigDecimal claimAmount) {
        ClaimRequestDto requestDto = new ClaimRequestDto();
        requestDto.setClaimId(claim.getClaimId());
        requestDto.setInvoiceId(claim.getInvoiceId());
        requestDto.setProviderName(claim.getProviderName());
        requestDto.setAmount(claimAmount);
        return claimClient.submitClaim(requestDto);
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
        charge.setCreatedAt(LocalDateTime.now().toInstant(java.time.ZoneOffset.UTC));
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
        charge.setCreatedAt(LocalDateTime.now().toInstant(java.time.ZoneOffset.UTC));
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
        charge.setCreatedAt(LocalDateTime.now().toInstant(java.time.ZoneOffset.UTC));
        unbilledChargeRepository.save(charge);
    }

    @Transactional
    public void finalizeDischargeBilling(UUID patientId, UUID admissionId, UUID doctorId) {
        List<UnbilledCharge> openCharges = unbilledChargeRepository.findByPatientIdAndStatus(patientId, "OPEN");
        
        if (openCharges.isEmpty()) {
            log.info("No open charges found for patient {} at discharge.", patientId);
            return;
        }

        BigDecimal totalAmount = openCharges.stream()
                .map(UnbilledCharge::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Generate final invoice for inpatient stay
        UUID invoiceId = UUID.randomUUID();
        String invoiceNumber = "IP-" + admissionId.toString().substring(0, 8);
        
        // [BACKLOG NOTE - ID 7]: Patient Insurance Enrichment Required at Discharge
        // Currently assuming NO_INSURANCE. When patient-service integration is added to
        // billing-service, fetch patient's default insurance here instead of BigDecimal.ZERO.
        String invoicePdfPath = invoiceService.generateInvoice(
                "Hospital Facility",
                "Patient " + patientId,
                totalAmount,
                invoiceNumber
        ).toAbsolutePath().toString();

        Invoice invoice = new Invoice();
        invoice.setInvoiceId(invoiceId);
        invoice.setDoctorId(doctorId); 
        invoice.setPatientId(patientId);
        invoice.setTotalAmount(totalAmount);
        
        invoice.setPatientOwes(totalAmount); 
        invoice.setInsuranceOwes(BigDecimal.ZERO);
        invoice.setInvoicePdfUrl(invoicePdfPath);
        invoiceRepository.save(invoice);

        // Mark all charges as BILLED
        for (UnbilledCharge charge : openCharges) {
            charge.setStatus("BILLED");
            unbilledChargeRepository.save(charge);
        }

        log.info("Final invoice {} generated for patient {} upon discharge.", invoiceNumber, patientId);
    }
}
