package com.project.notification_service.service;

import com.project.notification_service.model.NotificationHistory;
import com.project.notification_service.model.NotificationTemplate;
import com.project.notification_service.provider.NotificationProvider;
import com.project.notification_service.repository.NotificationHistoryRepository;
import com.project.notification_service.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationTemplateRepository templateRepository;

    @Mock
    private NotificationHistoryRepository historyRepository;

    @Mock
    private NotificationProvider emailProvider;

    @Mock
    private TemplateService templateService;

    private NotificationService notificationService;

    private final UUID patientId = UUID.randomUUID();
    private final String recipient = "test@example.com";
    private final String templateCode = "TEST_TEMPLATE";

    @BeforeEach
    void setUp() {
        lenient().when(emailProvider.getChannel()).thenReturn("EMAIL");
        notificationService = new NotificationService(
                templateRepository,
                historyRepository,
                List.of(emailProvider),
                templateService
        );
    }

    @Test
    void processNotification_Success() {
        // Arrange
        NotificationTemplate template = new NotificationTemplate();
        template.setTemplateCode(templateCode);
        template.setChannel("EMAIL");
        template.setSubject("Test Subject");
        template.setBody("Test Body");

        Map<String, Object> variables = Map.of("name", "John");

        when(templateRepository.findByTemplateCode(templateCode)).thenReturn(Optional.of(template));
        when(templateService.process(anyString(), eq(variables))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        notificationService.processNotification(patientId, recipient, templateCode, variables);

        // Assert
        verify(emailProvider).send(eq(recipient), anyString(), anyString());
        
        ArgumentCaptor<NotificationHistory> historyCaptor = ArgumentCaptor.forClass(NotificationHistory.class);
        verify(historyRepository).save(historyCaptor.capture());
        
        NotificationHistory history = historyCaptor.getValue();
        assertEquals(patientId, history.getPatientId());
        assertEquals(recipient, history.getRecipient());
        assertEquals("EMAIL", history.getChannel());
        assertEquals("SENT", history.getStatus());
    }

    @Test
    void processNotification_TemplateNotFound_ThrowsException() {
        // Arrange
        when(templateRepository.findByTemplateCode(templateCode)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
                notificationService.processNotification(patientId, recipient, templateCode, Collections.emptyMap()));
        
        verifyNoInteractions(emailProvider, templateService, historyRepository);
    }

    @Test
    void processNotification_ProviderNotFound_ThrowsException() {
        // Arrange
        NotificationTemplate template = new NotificationTemplate();
        template.setTemplateCode(templateCode);
        template.setChannel("SMS"); // No SMS provider configured in setup

        when(templateRepository.findByTemplateCode(templateCode)).thenReturn(Optional.of(template));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
                notificationService.processNotification(patientId, recipient, templateCode, Collections.emptyMap()));
        
        verifyNoInteractions(templateService, historyRepository);
    }

    @Test
    void processNotification_ProviderFailure_SavesFailedHistoryAndReThrows() {
        // Arrange
        NotificationTemplate template = new NotificationTemplate();
        template.setTemplateCode(templateCode);
        template.setChannel("EMAIL");
        template.setSubject("Test Subject");
        template.setBody("Test Body");

        when(templateRepository.findByTemplateCode(templateCode)).thenReturn(Optional.of(template));
        when(templateService.process(anyString(), any())).thenReturn("Processed");
        doThrow(new RuntimeException("Provider error")).when(emailProvider).send(anyString(), anyString(), anyString());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
                notificationService.processNotification(patientId, recipient, templateCode, Collections.emptyMap()));

        ArgumentCaptor<NotificationHistory> historyCaptor = ArgumentCaptor.forClass(NotificationHistory.class);
        verify(historyRepository).save(historyCaptor.capture());
        
        NotificationHistory history = historyCaptor.getValue();
        assertEquals("FAILED", history.getStatus());
        assertEquals("Provider error", history.getErrorLog());
    }
}
