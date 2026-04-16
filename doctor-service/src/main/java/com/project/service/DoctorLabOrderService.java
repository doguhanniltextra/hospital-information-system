package com.project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.dto.request.CreateLabOrderRequestDto;
import com.project.dto.response.CreateLabOrderResponseDto;
import com.project.model.DoctorLabOrder;
import com.project.model.PriorityLevel;
import com.project.model.DoctorOutboxEvent;
import com.project.repository.DoctorLabOrderRepository;
import com.project.repository.DoctorOutboxEventRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class DoctorLabOrderService {
    private final DoctorLabOrderRepository labOrderRepository;
    private final DoctorOutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public DoctorLabOrderService(
            DoctorLabOrderRepository labOrderRepository, 
            DoctorOutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper) {
        this.labOrderRepository = labOrderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CreateLabOrderResponseDto placeOrder(UUID doctorId, CreateLabOrderRequestDto request) {
        UUID orderId = UUID.randomUUID();
        BigDecimal total = request.getTests().stream()
                .map(t -> BigDecimal.valueOf(t.unitPrice == null ? 0D : t.unitPrice).multiply(BigDecimal.valueOf(t.quantity == null ? 1 : t.quantity)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DoctorLabOrder order = new DoctorLabOrder();
        order.setOrderId(orderId);
        order.setDoctorId(doctorId);
        order.setPatientId(request.getPatientId());
        order.setStatus("PLACED");
        order.setPriority(PriorityLevel.valueOf(request.getPriority()));
        order.setRequestedAt(request.getRequestedAt().toInstant(ZoneOffset.UTC));
        order.setOrderTotal(total);
        labOrderRepository.save(order);

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventId", UUID.randomUUID().toString());
            payload.put("eventVersion", "v1");
            payload.put("occurredAt", Instant.now().toString());
            payload.put("orderId", orderId);
            payload.put("patientId", request.getPatientId());
            payload.put("doctorId", doctorId);
            payload.put("tests", request.getTests());
            payload.put("orderTotal", total);
            payload.put("priority", request.getPriority());
            payload.put("correlationId", orderId.toString());

            DoctorOutboxEvent outbox = new DoctorOutboxEvent();
            outbox.setAggregateType("DoctorLabOrder");
            outbox.setAggregateId(orderId.toString());
            outbox.setEventType("LabOrderPlacedEvent");
            outbox.setPayloadJson(objectMapper.writeValueAsString(payload));
            outbox.setStatus("PENDING");
            outbox.setRetryCount(0);
            outbox.setNextRetryAt(Instant.now());
            outbox.setCreatedAt(Instant.now());
            outboxEventRepository.save(outbox);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize outbox payload", e);
        }

        CreateLabOrderResponseDto response = new CreateLabOrderResponseDto();
        response.setOrderId(orderId);
        response.setStatus("PLACED");
        return response;
    }
}
