package com.project.support_service.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.support_service.dto.LabOrderPlacedEvent;
import com.project.support_service.dto.LabResultCompletedEvent;
import com.project.support_service.model.lab.LabOrder;
import com.project.support_service.model.lab.LabOrderStatus;
import com.project.support_service.repository.LabOrderRepository;
import com.project.support_service.repository.SupportOutboxRepository;
import com.project.support_service.repository.TestResultRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LabCommandServiceTest {

    @Mock
    private LabOrderRepository labOrderRepository;
    @Mock
    private TestResultRepository testResultRepository;
    @Mock
    private SupportOutboxRepository outboxRepository;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private LabCommandService labCommandService;

    @Test
    public void createOrderFromEvent_ShouldSaveOrder() {
        LabOrderPlacedEvent event = new LabOrderPlacedEvent();
        event.orderId = UUID.randomUUID();
        event.priority = "HIGH"; // Fixed: PriorityLevel doesn't have URGENT, uses HIGH or STAT
        event.patientId = UUID.randomUUID();

        labCommandService.createOrderFromEvent(event);

        verify(labOrderRepository).save(any(LabOrder.class));
    }

    @Test
    public void startTest_ShouldUpdateStatus() {
        UUID orderId = UUID.randomUUID();
        LabOrder order = new LabOrder();
        order.setStatus(LabOrderStatus.QUEUED);

        when(labOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(labOrderRepository.save(any())).thenReturn(order);

        labCommandService.startTest(orderId);

        assertThat(order.getStatus()).isEqualTo(LabOrderStatus.IN_PROGRESS);
        assertThat(order.getStartedAt()).isNotNull();
    }

    @Test
    public void completeOrder_ShouldSaveResultsAndOutbox() throws JsonProcessingException {
        UUID orderId = UUID.randomUUID();
        LabOrder order = new LabOrder();
        order.setOrderId(orderId);
        order.setPatientId(UUID.randomUUID());

        when(labOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(labOrderRepository.save(any())).thenReturn(order);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        var results = new ArrayList<LabResultCompletedEvent.ResultItem>();
        results.add(new LabResultCompletedEvent.ResultItem());

        labCommandService.completeOrder(orderId, results, "http://report.pdf", "corr-123");

        assertThat(order.getStatus()).isEqualTo(LabOrderStatus.COMPLETED);
        verify(testResultRepository).save(any());
        verify(outboxRepository).save(any());
    }
}
