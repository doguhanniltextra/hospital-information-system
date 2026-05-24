package com.project.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.command.DoctorCommandService;
import com.project.command.IncreasePatientNumberCommand;
import com.project.exception.DoctorNotFoundException;
import com.project.exception.PatientLimitException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DoctorPatientCountConsumerTest {

    @Mock
    private DoctorCommandService doctorCommandService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private DoctorPatientCountConsumer consumer;

    @Test
    public void handlePatientCountIncrement_ShouldCallService() throws Exception {
        UUID doctorId = UUID.randomUUID();
        String message = "{\"doctorId\":\"" + doctorId + "\"}";

        consumer.handlePatientCountIncrement(message);

        verify(doctorCommandService).increasePatientNumber(any(IncreasePatientNumberCommand.class));
    }

    @Test
    public void handlePatientCountIncrement_WhenDoctorNotFound_ShouldLogAndNotThrow() throws Exception {
        UUID doctorId = UUID.randomUUID();
        String message = "{\"doctorId\":\"" + doctorId + "\"}";

        doThrow(new DoctorNotFoundException("Not found")).when(doctorCommandService).increasePatientNumber(any());

        consumer.handlePatientCountIncrement(message);

        verify(doctorCommandService).increasePatientNumber(any());
    }

    @Test
    public void handlePatientCountIncrement_WhenPatientLimitReached_ShouldLogAndNotThrow() throws Exception {
        UUID doctorId = UUID.randomUUID();
        String message = "{\"doctorId\":\"" + doctorId + "\"}";

        doThrow(new PatientLimitException("Limit full")).when(doctorCommandService).increasePatientNumber(any());

        consumer.handlePatientCountIncrement(message);

        verify(doctorCommandService).increasePatientNumber(any());
    }
}
