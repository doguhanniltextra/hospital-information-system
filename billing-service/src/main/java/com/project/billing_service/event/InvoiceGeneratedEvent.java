package com.project.billing_service.event;

import com.project.billing_service.model.Invoice;

public class InvoiceGeneratedEvent {

    private final Invoice invoice;

    public InvoiceGeneratedEvent(Invoice invoice) {
        this.invoice = invoice;
    }

    public Invoice getInvoice() {
        return invoice;
    }
}