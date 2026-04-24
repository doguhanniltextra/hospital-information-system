package com.project.support_service.controller;

import com.project.support_service.command.LabCommandService;
import com.project.support_service.constants.Endpoints;
import com.project.support_service.dto.LabResultCompletedEvent;
import com.project.support_service.model.lab.LabOrder;
import com.project.support_service.model.lab.LabOrderStatus;
import com.project.support_service.model.lab.LabTestCatalog;
import com.project.support_service.model.lab.TestResult;
import com.project.support_service.query.LabQueryService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.project.support_service.security.SecurityOwnershipService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Endpoints.LABS_BASE)
public class LabController {
    private final LabCommandService labCommandService;
    private final LabQueryService labQueryService;
    private final SecurityOwnershipService securityOwnershipService;

    public LabController(LabCommandService labCommandService, 
                         LabQueryService labQueryService,
                         SecurityOwnershipService securityOwnershipService) {
        this.labCommandService = labCommandService;
        this.labQueryService = labQueryService;
        this.securityOwnershipService = securityOwnershipService;
    }

    // --- Catalog ---
    @GetMapping(Endpoints.LABS_CATALOG)
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST','ADMIN','PATIENT')")
    public List<LabTestCatalog> getCatalog() {
        return labQueryService.getCatalog();
    }

    // --- Orders ---
    @GetMapping(Endpoints.LABS_ORDERS_ID)
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST','ADMIN') or (hasRole('PATIENT') and @securityOwnershipService.isLabOrderOwner(authentication, #orderId))")
    public LabOrder getOrder(@PathVariable UUID orderId) {
        return labQueryService.getOrder(orderId);
    }

    @GetMapping(Endpoints.LABS_ORDERS)
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST','ADMIN','PATIENT')")
    public List<LabOrder> listOrders(@RequestParam(required = false) UUID patientId, 
                                   @RequestParam(required = false) LabOrderStatus status,
                                   Authentication authentication) {
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"))) {
            UUID resolvedPatientId = securityOwnershipService.getPatientIdForAuthUser(authentication.getName());
            if (resolvedPatientId == null) {
                throw new AccessDeniedException("Patient identity not found for user");
            }
            return labQueryService.getOrdersByPatientAndStatus(resolvedPatientId, status);
        }

        if (patientId != null) return labQueryService.getOrdersByPatient(patientId);
        return labQueryService.getOrdersByStatus(status);
    }

    @PutMapping(Endpoints.LABS_ORDERS_ID_START)
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST','ADMIN')")
    public LabOrder start(@PathVariable UUID orderId) {
        return labCommandService.startTest(orderId);
    }

    @PutMapping(Endpoints.LABS_ORDERS_ID_COMPLETE)
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST','ADMIN')")
    public LabOrder complete(@PathVariable UUID orderId, @RequestBody CompleteOrderRequest request) {
        return labCommandService.completeOrder(orderId, request.results, request.reportPdfUrl, request.correlationId);
    }

    @GetMapping(Endpoints.LABS_ORDERS_ID_RESULTS)
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST','ADMIN') or (hasRole('PATIENT') and @securityOwnershipService.isLabOrderOwner(authentication, #orderId))")
    public List<TestResult> getResults(@PathVariable UUID orderId) {
        return labQueryService.getResults(orderId);
    }

    public static class CompleteOrderRequest {
        @NotNull public List<LabResultCompletedEvent.ResultItem> results;
        @NotBlank public String reportPdfUrl;
        public String correlationId;
    }
}
