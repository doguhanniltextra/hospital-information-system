package com.project.kafka;

import com.project.model.DoctorNotification;
import com.project.repository.DoctorNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LabResultCompletedConsumerTest {

    @Mock
    private DoctorNotificationRepository notificationRepository;

    @InjectMocks
    private LabResultCompletedConsumer consumer;

    @Test
    public void consume_ShouldSaveNotification() throws Exception {
        UUID doctorId = UUID.randomUUID();
        String eventId = "event-123";
        String orderId = "order-456";
        String message = String.format("{\"eventId\":\"%s\", \"doctorId\":\"%s\", \"orderId\":\"%s\"}", 
            eventId, doctorId, orderId);

        when(notificationRepository.findByEventId(eventId)).thenReturn(Optional.empty());

        consumer.consume(message);

        verify(notificationRepository).save(any(DoctorNotification.class));
    }

    @Test
    public void consume_WhenEventAlreadyProcessed_ShouldNotSaveDuplicate() throws Exception {
        String eventId = "event-123";
        String message = String.format("{\"eventId\":\"%s\"}", eventId);

        when(notificationRepository.findByEventId(eventId)).thenReturn(Optional.of(new DoctorNotification()));

        consumer.consume(message);

        verify(notificationRepository, never()).save(any());
    }
}
