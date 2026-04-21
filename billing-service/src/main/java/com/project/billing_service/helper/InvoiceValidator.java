package com.project.billing_service.helper;

import com.project.billing_service.constants.LogMessages;
import com.project.billing_service.command.InvoiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Component
public class InvoiceValidator {
    private static final Logger log = LoggerFactory.getLogger(InvoiceValidator.class);

    public  void createFailedToSaveInvoicePDFError(Path invoicePath, ResponseEntity<byte[]> response) {
        try {
            Files.write(invoicePath, response.getBody());
            log.info("PDF is created");
        } catch (IOException e) {
            throw new RuntimeException(LogMessages.FAILED_TO_SAVE_INVOICE_PDF, e);
        }
    }

    public  void createCannotCreateInvoiceDirectoryError(Path invoiceDir) {
        if (!Files.exists(invoiceDir)) {
            try {
                Files.createDirectories(invoiceDir);
            } catch (IOException e) {
                throw new RuntimeException(LogMessages.FAILED_TO_CREATE_INVOICE_DIR, e);
            }
        }
    }

    public  void createInvoiceGenerationError(ResponseEntity<byte[]> response) {
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException(LogMessages.FAILED_TO_CREATE_INVOICE_GENERATION);
        }
    }

    public  Map<String, Object> getStringObjectMapForItem(BigDecimal amount) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", "Medical Service");
        item.put("quantity", 1);
        item.put("unit_cost", amount);
        item.put("notes","Thanks for purchasing!");
        return item;
    }

    public  Map<String, Object> getStringObjectMapForBody(String doctorName, String patientName, String invoiceNumber) {
        Map<String, Object> body = new HashMap<>();
        body.put("from", doctorName);
        body.put("to", patientName);
        body.put("number", invoiceNumber);
        body.put("date", LocalDate.now().toString());
        body.put("due_date", LocalDate.now().plusDays(7).toString());
        return body;
    }
}
