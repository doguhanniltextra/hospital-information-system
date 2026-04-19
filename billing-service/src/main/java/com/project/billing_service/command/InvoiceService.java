package com.project.billing_service.command;

import com.project.billing_service.helper.InvoiceValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);
    private final RestTemplate restTemplate;
    private final InvoiceValidator invoiceValidator;

    @Value("${invoice.api.key}")
    private String apiKey;

    @Value("${invoice.api.url}")
    private String API_URL;

    @Value("${app.invoice.storage.path}")
    private String invoiceStoragePath;

    public InvoiceService(RestTemplate restTemplate, InvoiceValidator invoiceValidator) {
        this.restTemplate = restTemplate;
        this.invoiceValidator = invoiceValidator;
    }

    public Path generateInvoice(String doctorName, String patientName, BigDecimal amount, String invoiceNumber) {
        log.info("Generate Invoice Method Triggered");

        Map<String, Object> body = invoiceValidator.getStringObjectMapForBody(doctorName, patientName, invoiceNumber);

        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item = invoiceValidator.getStringObjectMapForItem(amount);
        items.add(item);
        body.put("items", items);
        log.info(items.toString());

        HttpHeaders headers = new HttpHeaders();

        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<byte[]> response = restTemplate.postForEntity(API_URL, request, byte[].class);

        invoiceValidator.createInvoiceGenerationError(response);

        Path invoiceDir = Paths.get(invoiceStoragePath);

        invoiceValidator.createCannotCreateInvoiceDirectoryError(invoiceDir);

        Path invoicePath = invoiceDir.resolve(invoiceNumber + ".pdf");

        invoiceValidator.createFailedToSaveInvoicePDFError(invoicePath, response);

        return invoicePath;
    }
}