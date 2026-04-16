package com.project.lab_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.lab_service.dto.LabOrderPlacedEvent;
import com.project.lab_service.dto.LabResultCompletedEvent;
import com.project.lab_service.model.LabOrder;
import com.project.lab_service.model.LabOrderStatus;
import com.project.lab_service.model.LabOutboxEvent;
import com.project.lab_service.model.TestResult;
import com.project.lab_service.repository.LabOrderRepository;
import com.project.lab_service.repository.LabOutboxRepository;
import com.project.lab_service.repository.TestResultRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class LabWorkflowService {
    private final LabOrderRepository labOrderRepository;
    private final TestResultRepository testResultRepository;
    private final LabOutboxRepository labOutboxRepository;
    private final ObjectMapper objectMapper;

    public LabWorkflowService(LabOrderRepository labOrderRepository, 
                              TestResultRepository testResultRepository, 
                              LabOutboxRepository labOutboxRepository,
                              ObjectMapper objectMapper) {
        this.labOrderRepository = labOrderRepository;
        this.testResultRepository = testResultRepository;
        this.labOutboxRepository = labOutboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void createFromPlacedEvent(LabOrderPlacedEvent event) {
        LabOrder order = new LabOrder();
        order.setOrderId(event.orderId);
        order.setDoctorId(event.doctorId);
        order.setPatientId(event.patientId);
        order.setPatientEmail(event.patientEmail);
        order.setPatientPhone(event.patientPhone);
        order.setRequestedAt(event.occurredAt != null ? event.occurredAt : Instant.now());
        order.setStatus(LabOrderStatus.QUEUED);
        try {
            order.setPriority(com.project.lab_service.model.PriorityLevel.valueOf(event.priority));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid or missing priority: " + event.priority);
        }
        order.setTotalAmount(event.orderTotal);
        labOrderRepository.save(order);
    }

    @Transactional
    public LabOrder start(UUID orderId) {
        LabOrder order = labOrderRepository.findById(orderId).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        order.setStatus(LabOrderStatus.IN_PROGRESS);
        order.setStartedAt(Instant.now());
        return labOrderRepository.save(order);
    }

    @Transactional
    public LabOrder complete(UUID orderId, List<LabResultCompletedEvent.ResultItem> results, String reportUrl, String correlationId) {
        LabOrder order = labOrderRepository.findById(orderId).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        order.setStatus(LabOrderStatus.COMPLETED);
        order.setCompletedAt(Instant.now());
        LabOrder saved = labOrderRepository.save(order);

        for (LabResultCompletedEvent.ResultItem item : results) {
            TestResult result = new TestResult();
            result.setOrderId(orderId);
            result.setTestCode(item.testCode);
            result.setValue(item.value);
            result.setUnit(item.unit);
            result.setReferenceRange(item.referenceRange);
            result.setAbnormalFlag(item.abnormalFlag);
            result.setReportPdfUrl(reportUrl);
            result.setValidatedAt(Instant.now());
            testResultRepository.save(result);
        }

        enqueueOutboxEvent(saved, results, reportUrl, correlationId);
        return saved;
    }

    private void enqueueOutboxEvent(LabOrder order, List<LabResultCompletedEvent.ResultItem> items, String reportUrl, String correlationId) {
        LabResultCompletedEvent event = new LabResultCompletedEvent();
        event.eventId = UUID.randomUUID().toString();
        event.eventVersion = "v1";
        event.occurredAt = Instant.now();
        event.orderId = order.getOrderId();
        event.patientId = order.getPatientId();
        event.patientEmail = order.getPatientEmail();
        event.patientPhone = order.getPatientPhone();
        event.doctorId = order.getDoctorId();
        event.results = items;
        event.reportPdfUrl = reportUrl;
        event.completedAt = order.getCompletedAt();
        event.correlationId = correlationId;

        try {
            LabOutboxEvent outboxEvent = new LabOutboxEvent();
            outboxEvent.setAggregateType("LAB_ORDER");
            outboxEvent.setAggregateId(order.getOrderId().toString());
            outboxEvent.setEventType("LAB_RESULT_COMPLETED");
            outboxEvent.setPayloadJson(objectMapper.writeValueAsString(event));
            labOutboxRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize lab result event for outbox", e);
        }
    }
}
