package com.project.support_service.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.support_service.dto.LabOrderPlacedEvent;
import com.project.support_service.dto.LabResultCompletedEvent;
import com.project.support_service.model.lab.LabOrder;
import com.project.support_service.model.lab.LabOrderStatus;
import com.project.support_service.model.lab.PriorityLevel;
import com.project.support_service.model.lab.TestResult;
import com.project.support_service.model.outbox.SupportOutboxEvent;
import com.project.support_service.repository.LabOrderRepository;
import com.project.support_service.repository.SupportOutboxRepository;
import com.project.support_service.repository.TestResultRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class LabCommandService {
    private final LabOrderRepository labOrderRepository;
    private final TestResultRepository testResultRepository;
    private final SupportOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public LabCommandService(LabOrderRepository labOrderRepository,
                             TestResultRepository testResultRepository,
                             SupportOutboxRepository outboxRepository,
                             ObjectMapper objectMapper) {
        this.labOrderRepository = labOrderRepository;
        this.testResultRepository = testResultRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void createOrderFromEvent(LabOrderPlacedEvent event) {
        LabOrder order = new LabOrder();
        order.setOrderId(event.orderId);
        order.setDoctorId(event.doctorId);
        order.setPatientId(event.patientId);
        order.setPatientEmail(event.patientEmail);
        order.setPatientPhone(event.patientPhone);
        order.setRequestedAt(event.occurredAt != null ? event.occurredAt : Instant.now());
        order.setStatus(LabOrderStatus.QUEUED);
        order.setPriority(PriorityLevel.valueOf(event.priority));
        order.setTotalAmount(event.orderTotal);
        labOrderRepository.save(order);
    }

    @Transactional
    @CacheEvict(value = "labOrders", allEntries = true)
    public LabOrder startTest(UUID orderId) {
        LabOrder order = labOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        order.setStatus(LabOrderStatus.IN_PROGRESS);
        order.setStartedAt(Instant.now());
        return labOrderRepository.save(order);
    }

    @Transactional
    @CacheEvict(value = "labOrders", allEntries = true)
    public LabOrder completeOrder(UUID orderId, List<LabResultCompletedEvent.ResultItem> results, String reportUrl, String correlationId) {
        LabOrder order = labOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        
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
        event.doctorId = order.getDoctorId();
        event.results = items;
        event.reportPdfUrl = reportUrl;
        event.completedAt = order.getCompletedAt();
        event.correlationId = correlationId;

        try {
            SupportOutboxEvent outboxEvent = new SupportOutboxEvent(
                    "LAB_ORDER",
                    order.getOrderId().toString(),
                    "lab-result-completed.v1",
                    objectMapper.writeValueAsString(event)
            );
            outboxRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize lab result event", e);
        }
    }
}
