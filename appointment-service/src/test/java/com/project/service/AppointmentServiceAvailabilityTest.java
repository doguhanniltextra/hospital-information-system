package com.project.appointment_service.service;

import com.project.appointment_service.dto.DoctorAvailabilityResponseDTO;
import com.project.appointment_service.dto.request.CreateAppointmentServiceRequestDto;
import com.project.appointment_service.exception.CustomConflictException;
import com.project.appointment_service.helper.AppointmentMapper;
import com.project.appointment_service.helper.AppointmentValidator;
import com.project.appointment_service.kafka.KafkaProducer;
import com.project.appointment_service.model.Appointment;
import com.project.appointment_service.model.PaymentType;
import com.project.appointment_service.model.ServiceType;
import com.project.appointment_service.repository.AppointmentRepository;
import com.project.appointment_service.utils.IdValidation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AppointmentServiceAvailabilityTest {

    @Mock
    private KafkaProducer kafkaProducer;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private IdValidation idValidation;
    @Mock
    private AppointmentMapper appointmentMapper;
    @Mock
    private AppointmentValidator appointmentValidator;

    @InjectMocks
    private AppointmentService appointmentService;

    @Test
    public void createAppointment_WhenDoctorUnavailable_ThrowsConflict() {
        CreateAppointmentServiceRequestDto dto = new CreateAppointmentServiceRequestDto();
        dto.setId(UUID.randomUUID());
        dto.setDoctorId(UUID.randomUUID());
        dto.setPatientId(UUID.randomUUID());
        dto.setServiceDate("2026-11-12 10:00");
        dto.setServiceDateEnd("2026-11-12 10:30");
        dto.setServiceType(ServiceType.CONSULTATION);
        dto.setAmount(200);
        dto.setPaymentType(PaymentType.DEBIT);
        dto.setPaymentStatus(false);

        doNothing().when(appointmentValidator).checkPatientExistsOrNotForCreateAppointment(dto.getPatientId());
        doNothing().when(appointmentValidator).checkDoctorExistsOrNotForCreateAppointment(dto.getDoctorId());

        DoctorAvailabilityResponseDTO availabilityResponse = new DoctorAvailabilityResponseDTO();
        availabilityResponse.setAvailable(false);
        availabilityResponse.setReasonCode("ON_LEAVE");
        when(idValidation.checkDoctorAvailability(
                eq(dto.getDoctorId()),
                eq(dto.getServiceDate()),
                eq(dto.getServiceDateEnd()),
                eq(dto.getServiceType())
        )).thenReturn(availabilityResponse);

        assertThatThrownBy(() -> appointmentService.createAppointment(dto))
                .isInstanceOf(CustomConflictException.class)
                .hasMessageContaining("ON_LEAVE");

        verify(idValidation).checkDoctorAvailability(any(), any(), any(), any());
    }
}
