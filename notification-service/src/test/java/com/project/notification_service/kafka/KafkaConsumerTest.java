package com.project.notification_service.kafka;

import com.project.notification_service.dto.AppointmentScheduledEvent;
import com.project.notification_service.dto.LabResultCompletedEvent;
import com.project.notification_service.dto.PatientContactInfo;
import com.project.notification_service.grpc.PatientGrpcClient;
import com.project.notification_service.model.NotificationProcessedEvent;
import com.project.notification_service.repository.NotificationProcessedEventRepository;
import com.project.notification_service.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private PatientGrpcClient patientGrpcClient;

    @Mock
    private NotificationProcessedEventRepository processedEventRepository;

    private KafkaConsumer kafkaConsumer;

    private final String opsAlertEmails = "admin@hospital.com";

    @BeforeEach
    void setUp() {
        kafkaConsumer = new KafkaConsumer(
                notificationService,
                patientGrpcClient,
                processedEventRepository,
                opsAlertEmails
        );
    }

    @Test
    void consumeLabResult_Success() {
        // Arrange
        UUID patientId = UUID.randomUUID();
        LabResultCompletedEvent event = new LabResultCompletedEvent();
        event.eventId = "EVT-123";
        event.patientId = patientId;
        event.reportPdfUrl = "http://lab.com/report.pdf";

        PatientContactInfo contactInfo = new PatientContactInfo(patientId.toString(), "John Doe", "john@example.com", "123", "Addr");

        when(processedEventRepository.existsByMessageId(event.eventId)).thenReturn(false);
        when(patientGrpcClient.getPatientContactInfo(patientId)).thenReturn(contactInfo);

        // Act
        kafkaConsumer.consumeLabResult(event);

        // Assert
        verify(notificationService).processNotification(eq(patientId), eq("john@example.com"), eq("LAB_RESULT_READY"), any());
        verify(processedEventRepository).save(any(NotificationProcessedEvent.class));
    }

    @Test
    void consumeLabResult_AlreadyProcessed_Skips() {
        // Arrange
        LabResultCompletedEvent event = new LabResultCompletedEvent();
        event.eventId = "EVT-123";

        when(processedEventRepository.existsByMessageId(event.eventId)).thenReturn(true);

        // Act
        kafkaConsumer.consumeLabResult(event);

        // Assert
        verifyNoInteractions(notificationService, patientGrpcClient);
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void consumeAppointment_Success() {
        // Arrange
        UUID patientId = UUID.randomUUID();
        AppointmentScheduledEvent event = new AppointmentScheduledEvent();
        event.patientId = patientId;
        event.appointmentDate = "2023-10-10";
        event.doctorId = UUID.randomUUID();

        when(processedEventRepository.existsByMessageId(anyString())).thenReturn(false);
        when(patientGrpcClient.getPatientContactInfo(patientId)).thenReturn(null); // Should use fallback email

        // Act
        kafkaConsumer.consumeAppointment(event);

        // Assert
        verify(notificationService).processNotification(eq(patientId), contains(patientId.toString()), eq("APPOINTMENT_CONFIRMATION"), any());
        verify(processedEventRepository).save(any(NotificationProcessedEvent.class));
    }
}
