package com.project.lab_service.controller;

import com.project.lab_service.constants.Endpoints;
import com.project.lab_service.dto.LabResultCompletedEvent;
import com.project.lab_service.model.LabOrder;
import com.project.lab_service.model.LabOrderStatus;
import com.project.lab_service.repository.LabOrderRepository;
import com.project.lab_service.repository.TestResultRepository;
import com.project.lab_service.service.LabWorkflowService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Endpoints.LAB_ORDERS)
public class LabOrderController {
    private final LabOrderRepository labOrderRepository;
    private final TestResultRepository testResultRepository;
    private final LabWorkflowService workflowService;

    public LabOrderController(LabOrderRepository labOrderRepository, TestResultRepository testResultRepository, LabWorkflowService workflowService) {
        this.labOrderRepository = labOrderRepository;
        this.testResultRepository = testResultRepository;
        this.workflowService = workflowService;
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST','ADMIN','PATIENT')")
    public LabOrder getOrder(@PathVariable UUID orderId) {
        return labOrderRepository.findById(orderId).orElseThrow();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST','ADMIN','PATIENT')")
    public List<LabOrder> listOrders(@RequestParam(required = false) UUID patientId, @RequestParam(required = false) LabOrderStatus status) {
        if (patientId != null && status != null) {
            return labOrderRepository.findByPatientIdAndStatusOrderByPriorityDescRequestedAtAsc(patientId, status);
        }
        if (patientId != null) {
            return labOrderRepository.findByPatientIdOrderByPriorityDescRequestedAtAsc(patientId);
        }
        if (status != null) {
            return labOrderRepository.findByStatusOrderByPriorityDescRequestedAtAsc(status);
        }
        return labOrderRepository.findAllByOrderByPriorityDescRequestedAtAsc();
    }

    @PutMapping("/{orderId}/start")
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST','ADMIN')")
    public LabOrder start(@PathVariable UUID orderId) {
        return workflowService.start(orderId);
    }

    @PutMapping("/{orderId}/complete")
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST','ADMIN')")
    public LabOrder complete(@PathVariable UUID orderId, @RequestBody CompleteOrderRequest request) {
        return workflowService.complete(orderId, request.results, request.reportPdfUrl, request.correlationId);
    }

    @GetMapping("/{orderId}/results")
    @PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST','ADMIN','PATIENT')")
    public Object getResults(@PathVariable UUID orderId) {
        return testResultRepository.findByOrderId(orderId);
    }

    public static class CompleteOrderRequest {
        @NotNull
        public List<LabResultCompletedEvent.ResultItem> results;
        @NotBlank
        public String reportPdfUrl;
        public String correlationId;
    }
}
