package com.project.billing_service.eventhandler;

import com.project.billing_service.event.InvoiceGeneratedEvent;
import com.project.billing_service.model.Invoice;
import com.project.billing_service.readmodel.InvoiceSummary;
import com.project.billing_service.readmodel.InvoiceSummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Component
public class BillingEventHandler {

    private static final Logger log = LoggerFactory.getLogger(BillingEventHandler.class);
    private final InvoiceSummaryRepository invoiceSummaryRepository;

    public BillingEventHandler(InvoiceSummaryRepository invoiceSummaryRepository) {
        this.invoiceSummaryRepository = invoiceSummaryRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleInvoiceGenerated(InvoiceGeneratedEvent event) {
        log.info("Synchronizing new invoice to Read Model: {}", event.getInvoice().getInvoiceId());
        upsertSummary(event.getInvoice());
    }

    private void upsertSummary(Invoice invoice) {
        InvoiceSummary summary = invoiceSummaryRepository.findById(invoice.getInvoiceId())
                .orElse(new InvoiceSummary());

        summary.setInvoiceId(invoice.getInvoiceId());
        summary.setPatientId(invoice.getPatientId());
        summary.setDoctorId(invoice.getDoctorId());
        summary.setTotalAmount(invoice.getTotalAmount());
        summary.setPatientOwes(invoice.getPatientOwes());
        summary.setInsuranceOwes(invoice.getInsuranceOwes());
        summary.setStatus("PENDING"); // Default status

        // TODO: Fetch and set patientName and doctorName from other services if needed
        // For now, set placeholders or fetch via client calls
        summary.setPatientName("Patient " + invoice.getPatientId());
        summary.setDoctorName("Doctor " + invoice.getDoctorId());

        summary.setCreatedAt(LocalDateTime.now());
        summary.setLastUpdated(LocalDateTime.now());

        invoiceSummaryRepository.save(summary);
    }
}