package com.project.support_service.controller;

import com.project.support_service.command.LabCommandService;
import com.project.support_service.dto.LabResultCompletedEvent;
import com.project.support_service.model.lab.LabOrder;
import com.project.support_service.model.lab.LabOrderStatus;
import com.project.support_service.model.lab.LabTestCatalog;
import com.project.support_service.model.lab.TestResult;
import com.project.support_service.query.LabQueryService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/labs")
public class LabController {
    private final LabCommandService labCommandService;
    private final LabQueryService labQueryService;

    public LabController(LabCommandService labCommandService, LabQueryService labQueryService) {
        this.labCommandService = labCommandService;
        this.labQueryService = labQueryService;
    }

    // --- Catalog ---
    @GetMapping("/catalog")
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST','ADMIN','PATIENT')")
    public List<LabTestCatalog> getCatalog() {
        return labQueryService.getCatalog();
    }

    // --- Orders ---
    @GetMapping("/orders/{orderId}")
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST','ADMIN','PATIENT')")
    public LabOrder getOrder(@PathVariable UUID orderId) {
        return labQueryService.getOrder(orderId);
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST','ADMIN','PATIENT')")
    public List<LabOrder> listOrders(@RequestParam(required = false) UUID patientId, 
                                   @RequestParam(required = false) LabOrderStatus status) {
        if (patientId != null) return labQueryService.getOrdersByPatient(patientId);
        if (status != null) return labQueryService.getOrdersByStatus(status);
        return labQueryService.getOrdersByStatus(null); // Fallback to all if status logic allows
    }

    @PutMapping("/orders/{orderId}/start")
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST','ADMIN')")
    public LabOrder start(@PathVariable UUID orderId) {
        return labCommandService.startTest(orderId);
    }

    @PutMapping("/orders/{orderId}/complete")
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST','ADMIN')")
    public LabOrder complete(@PathVariable UUID orderId, @RequestBody CompleteOrderRequest request) {
        return labCommandService.completeOrder(orderId, request.results, request.reportPdfUrl, request.correlationId);
    }

    @GetMapping("/orders/{orderId}/results")
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST','ADMIN','PATIENT')")
    public List<TestResult> getResults(@PathVariable UUID orderId) {
        return labQueryService.getResults(orderId);
    }

    public static class CompleteOrderRequest {
        @NotNull public List<LabResultCompletedEvent.ResultItem> results;
        @NotBlank public String reportPdfUrl;
        public String correlationId;
    }
}
