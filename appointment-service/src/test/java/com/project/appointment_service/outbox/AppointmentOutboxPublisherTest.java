package com.project.appointment_service.outbox;

import com.project.appointment_service.kafka.KafkaProducer;
import com.project.appointment_service.model.AppointmentOutboxEvent;
import com.project.appointment_service.repository.AppointmentOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentOutboxPublisher")
class AppointmentOutboxPublisherTest {

    @Mock private AppointmentOutboxRepository outboxRepository;
    @Mock private KafkaProducer kafkaProducer;

    @InjectMocks private AppointmentOutboxPublisher publisher;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private AppointmentOutboxEvent pendingEvent(String eventType) {
        return new AppointmentOutboxEvent("agg-1", "APPOINTMENT", eventType, "{\"key\":\"value\"}");
    }

    // ── Happy path – topic routing ────────────────────────────────────────────

    @Test
    @DisplayName("publishEvents() routes APPOINTMENT_CREATED to doctor-patient-count-update.v1")
    void publishEvents_appointmentCreated_routesToCorrectTopic() {
        AppointmentOutboxEvent event = pendingEvent("APPOINTMENT_CREATED");
        when(outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(event));

        publisher.publishEvents();

        verify(kafkaProducer).sendRawEvent("doctor-patient-count-update.v1", event.getPayload());
    }

    @Test
    @DisplayName("publishEvents() routes PAYMENT_UPDATE to appointment-payment-updates.v1")
    void publishEvents_paymentUpdate_routesToCorrectTopic() {
        AppointmentOutboxEvent event = pendingEvent("PAYMENT_UPDATE");
        when(outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(event));

        publisher.publishEvents();

        verify(kafkaProducer).sendRawEvent("appointment-payment-updates.v1", event.getPayload());
    }

    @Test
    @DisplayName("publishEvents() routes unknown event types to appointment-events.v1")
    void publishEvents_unknownEventType_routesToFallbackTopic() {
        AppointmentOutboxEvent event = pendingEvent("SOME_OTHER_EVENT");
        when(outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(event));

        publisher.publishEvents();

        verify(kafkaProducer).sendRawEvent("appointment-events.v1", event.getPayload());
    }

    // ── State transitions ─────────────────────────────────────────────────────

    @Test
    @DisplayName("publishEvents() marks a successfully published event as PROCESSED")
    void publishEvents_onSuccess_marksEventAsProcessed() {
        AppointmentOutboxEvent event = pendingEvent("APPOINTMENT_CREATED");
        when(outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(event));

        publisher.publishEvents();

        assertThat(event.getStatus()).isEqualTo("PROCESSED");
        assertThat(event.getProcessedAt()).isNotNull();
        verify(outboxRepository).save(event);
    }

    @Test
    @DisplayName("publishEvents() keeps event as PENDING and does NOT save when Kafka throws")
    void publishEvents_onKafkaFailure_doesNotMarkProcessed() {
        AppointmentOutboxEvent event = pendingEvent("APPOINTMENT_CREATED");
        when(outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(event));
        doThrow(new RuntimeException("Kafka unavailable"))
                .when(kafkaProducer).sendRawEvent(any(), any());

        publisher.publishEvents(); // should NOT propagate

        assertThat(event.getStatus()).isEqualTo("PENDING");
        verify(outboxRepository, never()).save(event);
    }

    // ── Short-circuit ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("publishEvents() does nothing when no pending events exist")
    void publishEvents_noPendingEvents_skipsKafkaAndSave() {
        when(outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(Collections.emptyList());

        publisher.publishEvents();

        verifyNoInteractions(kafkaProducer);
        verify(outboxRepository, never()).save(any());
    }

    // ── Multi-event batch ─────────────────────────────────────────────────────

    @Test
    @DisplayName("publishEvents() processes all events in a batch")
    void publishEvents_multiplePendingEvents_allPublished() {
        AppointmentOutboxEvent e1 = pendingEvent("APPOINTMENT_CREATED");
        AppointmentOutboxEvent e2 = pendingEvent("PAYMENT_UPDATE");
        when(outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(e1, e2));

        publisher.publishEvents();

        verify(kafkaProducer).sendRawEvent(eq("doctor-patient-count-update.v1"), any());
        verify(kafkaProducer).sendRawEvent(eq("appointment-payment-updates.v1"), any());
        verify(outboxRepository, times(2)).save(any());
    }
}
