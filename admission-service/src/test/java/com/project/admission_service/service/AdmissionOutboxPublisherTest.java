package com.project.admission_service.service;

import com.project.admission_service.model.AdmissionOutboxEvent;
import com.project.admission_service.repository.AdmissionOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AdmissionOutboxPublisherTest {

    @Mock
    private AdmissionOutboxRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private AdmissionOutboxPublisher outboxPublisher;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(outboxPublisher, "bedChargeTopic", "admission-bed-charge.v1");
        ReflectionTestUtils.setField(outboxPublisher, "patientDischargedTopic", "admission-discharged.v1");
    }

    @Test
    void publishEvents_DailyBedCharge_Success() {
        UUID eventId = UUID.randomUUID();
        AdmissionOutboxEvent event = new AdmissionOutboxEvent();
        event.setId(eventId);
        event.setAggregateId("agg-1");
        event.setEventType("DAILY_BED_CHARGE");
        event.setPayloadJson("{\"amount\":1500}");
        event.setStatus("PENDING");

        when(outboxRepository.findByStatus("PENDING")).thenReturn(Collections.singletonList(event));
        when(kafkaTemplate.send(eq("admission-bed-charge.v1"), eq("agg-1"), eq("{\"amount\":1500}")))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(outboxRepository.findById(eventId)).thenReturn(Optional.of(event));

        outboxPublisher.publishEvents();

        verify(kafkaTemplate, times(1)).send("admission-bed-charge.v1", "agg-1", "{\"amount\":1500}");
        verify(outboxRepository, times(1)).save(event);
    }
}
