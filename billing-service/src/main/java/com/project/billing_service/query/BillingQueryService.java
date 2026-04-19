package com.project.billing_service.query;

import com.project.billing_service.readmodel.InvoiceSummary;
import com.project.billing_service.readmodel.InvoiceSummaryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BillingQueryService {

    private final InvoiceSummaryRepository invoiceSummaryRepository;

    public BillingQueryService(InvoiceSummaryRepository invoiceSummaryRepository) {
        this.invoiceSummaryRepository = invoiceSummaryRepository;
    }

    public List<InvoiceSummary> getAllInvoices() {
        return invoiceSummaryRepository.findAll();
    }

    public Optional<InvoiceSummary> getInvoiceById(UUID invoiceId) {
        return invoiceSummaryRepository.findById(invoiceId);
    }

    public List<InvoiceSummary> getInvoicesByPatientId(UUID patientId) {
        return invoiceSummaryRepository.findByPatientId(patientId);
    }

    public List<InvoiceSummary> getInvoicesByStatus(String status) {
        return invoiceSummaryRepository.findByStatus(status);
    }
}