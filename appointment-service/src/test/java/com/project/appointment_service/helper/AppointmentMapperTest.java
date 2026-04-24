package com.project.appointment_service.helper;

import com.project.appointment_service.dto.request.CreateAppointmentServiceRequestDto;
import com.project.appointment_service.dto.response.CreateAppointmentServiceResponseDto;
import com.project.appointment_service.model.Appointment;
import com.project.appointment_service.model.PaymentType;
import com.project.appointment_service.model.ServiceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class AppointmentMapperTest {

    private AppointmentMapper appointmentMapper;

    @BeforeEach
    public void setUp() {
        appointmentMapper = new AppointmentMapper();
    }

    @Test
    public void getCreateAppointmentServiceResponseDto_ShouldMapCorrectly() {
        CreateAppointmentServiceRequestDto request = createRequest();

        CreateAppointmentServiceResponseDto response = appointmentMapper.getCreateAppointmentServiceResponseDto(request);

        assertThat(response.getId()).isEqualTo(request.getId());
        assertThat(response.getDoctorId()).isEqualTo(request.getDoctorId());
        assertThat(response.getPatientId()).isEqualTo(request.getPatientId());
        assertThat(response.getAmount()).isEqualTo(request.getAmount());
        assertThat(response.getServiceDate()).isEqualTo(request.getServiceDate());
        assertThat(response.getServiceDateEnd()).isEqualTo(request.getServiceDateEnd());
        assertThat(response.isPaymentStatus()).isEqualTo(request.isPaymentStatus());
        assertThat(response.getPaymentType()).isEqualTo(request.getPaymentType());
    }

    @Test
    public void getAppointment_ShouldMapCorrectly() {
        CreateAppointmentServiceRequestDto request = createRequest();

        Appointment appointment = appointmentMapper.getAppointment(request);

        assertThat(appointment.getId()).isEqualTo(request.getId());
        assertThat(appointment.getDoctorId()).isEqualTo(request.getDoctorId());
        assertThat(appointment.getPatientId()).isEqualTo(request.getPatientId());
        assertThat(appointment.getAmount()).isEqualTo(request.getAmount());
        assertThat(appointment.getServiceDate()).isEqualTo(request.getServiceDate());
        assertThat(appointment.getServiceDateEnd()).isEqualTo(request.getServiceDateEnd());
        assertThat(appointment.isPaymentStatus()).isEqualTo(request.isPaymentStatus());
        assertThat(appointment.getPaymentType()).isEqualTo(request.getPaymentType());
        assertThat(appointment.getServiceType()).isEqualTo(request.getServiceType());
    }

    @Test
    public void updateAppointmentExtracted_ShouldUpdateExistingFields() {
        Appointment source = new Appointment();
        source.setDoctorId(UUID.randomUUID());
        source.setPatientId(UUID.randomUUID());
        source.setAmount(500);
        source.setServiceDate("2026-12-01 15:00");
        source.setServiceType(ServiceType.SURGERY);
        source.setPaymentStatus(true);
        source.setPaymentType(PaymentType.CREDIT);

        Appointment existing = new Appointment();
        UUID existingId = UUID.randomUUID();
        existing.setId(existingId);

        appointmentMapper.updateAppointmentExtracted(source, existing);

        assertThat(existing.getId()).isEqualTo(existingId); // Should NOT change
        assertThat(existing.getDoctorId()).isEqualTo(source.getDoctorId());
        assertThat(existing.getPatientId()).isEqualTo(source.getPatientId());
        assertThat(existing.getAmount()).isEqualTo(source.getAmount());
        assertThat(existing.getServiceDate()).isEqualTo(source.getServiceDate());
        assertThat(existing.getServiceType()).isEqualTo(source.getServiceType());
        assertThat(existing.isPaymentStatus()).isTrue();
        assertThat(existing.getPaymentType()).isEqualTo(source.getPaymentType());
    }

    @Test
    public void getCreateAppointmentServiceRequestDto_ShouldMapCorrectly() {
        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setDoctorId(UUID.randomUUID());
        appointment.setPatientId(UUID.randomUUID());
        appointment.setAmount(150);
        appointment.setServiceDate("2026-11-20 09:00");
        appointment.setServiceDateEnd("2026-11-20 09:30");
        appointment.setServiceType(ServiceType.VACCINATION);
        appointment.setPaymentStatus(false);
        appointment.setPaymentType(PaymentType.CREDIT);

        CreateAppointmentServiceRequestDto request = appointmentMapper.getCreateAppointmentServiceRequestDto(appointment);

        assertThat(request.getId()).isEqualTo(appointment.getId());
        assertThat(request.getDoctorId()).isEqualTo(appointment.getDoctorId());
        assertThat(request.getPatientId()).isEqualTo(appointment.getPatientId());
        assertThat(request.getAmount()).isEqualTo(appointment.getAmount());
        assertThat(request.getServiceDate()).isEqualTo(appointment.getServiceDate());
        assertThat(request.getServiceDateEnd()).isEqualTo(appointment.getServiceDateEnd());
        assertThat(request.getServiceType()).isEqualTo(appointment.getServiceType());
        assertThat(request.isPaymentStatus()).isFalse();
        assertThat(request.getPaymentType()).isEqualTo(appointment.getPaymentType());
    }

    private CreateAppointmentServiceRequestDto createRequest() {
        CreateAppointmentServiceRequestDto request = new CreateAppointmentServiceRequestDto();
        request.setId(UUID.randomUUID());
        request.setDoctorId(UUID.randomUUID());
        request.setPatientId(UUID.randomUUID());
        request.setAmount(250);
        request.setServiceDate("2026-12-25 10:00");
        request.setServiceDateEnd("2026-12-25 10:30");
        request.setPaymentStatus(false);
        request.setPaymentType(PaymentType.DEBIT);
        request.setServiceType(ServiceType.CONSULTATION);
        return request;
    }
}
