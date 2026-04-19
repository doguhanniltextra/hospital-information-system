package com.project.appointment_service.helper;

import com.project.appointment_service.dto.request.CreateAppointmentServiceRequestDto;
import com.project.appointment_service.dto.response.CreateAppointmentServiceResponseDto;
import com.project.appointment_service.model.Appointment;
import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {

    public CreateAppointmentServiceResponseDto getCreateAppointmentServiceResponseDto(CreateAppointmentServiceRequestDto createAppointmentServiceRequestDto) {
        CreateAppointmentServiceResponseDto appointmentServiceResponseDto = new CreateAppointmentServiceResponseDto();
        appointmentServiceResponseDto.setDoctorId(createAppointmentServiceRequestDto.getDoctorId());
        appointmentServiceResponseDto.setPatientId(createAppointmentServiceRequestDto.getPatientId());
        appointmentServiceResponseDto.setAmount(createAppointmentServiceRequestDto.getAmount());
        appointmentServiceResponseDto.setId(createAppointmentServiceRequestDto.getId());
        appointmentServiceResponseDto.setServiceDate(createAppointmentServiceRequestDto.getServiceDate());
        appointmentServiceResponseDto.setPaymentStatus(createAppointmentServiceRequestDto.isPaymentStatus());
        appointmentServiceResponseDto.setServiceDateEnd(createAppointmentServiceRequestDto.getServiceDateEnd());
        appointmentServiceResponseDto.setPaymentType(createAppointmentServiceRequestDto.getPaymentType());
        // Note: status is usually set by the caller (Saga) or we can default it here
        return appointmentServiceResponseDto;
    }

    public Appointment getAppointment(CreateAppointmentServiceRequestDto createAppointmentServiceRequestDto) {
        Appointment appointment = new Appointment();
        appointment.setId(createAppointmentServiceRequestDto.getId());
        appointment.setAmount(createAppointmentServiceRequestDto.getAmount());
        appointment.setPaymentStatus(createAppointmentServiceRequestDto.isPaymentStatus());
        appointment.setDoctorId(createAppointmentServiceRequestDto.getDoctorId());
        appointment.setPatientId(createAppointmentServiceRequestDto.getPatientId());
        appointment.setPaymentType(createAppointmentServiceRequestDto.getPaymentType());
        appointment.setServiceDate(createAppointmentServiceRequestDto.getServiceDate());
        appointment.setServiceDateEnd(createAppointmentServiceRequestDto.getServiceDateEnd());
        appointment.setServiceType(createAppointmentServiceRequestDto.getServiceType());
        return appointment;
    }

    public void updateAppointmentExtracted(Appointment appointment, Appointment existingAppointment) {
        existingAppointment.setPatientId(appointment.getPatientId());
        existingAppointment.setDoctorId(appointment.getDoctorId());
        existingAppointment.setServiceDate(appointment.getServiceDate());
        existingAppointment.setServiceType(appointment.getServiceType());
        existingAppointment.setAmount(appointment.getAmount());
        existingAppointment.setPaymentStatus(appointment.isPaymentStatus());
        existingAppointment.setPaymentType(appointment.getPaymentType());
    }

    public CreateAppointmentServiceRequestDto getCreateAppointmentServiceRequestDto(Appointment appointment) {
        CreateAppointmentServiceRequestDto requestDto = new CreateAppointmentServiceRequestDto();
        requestDto.setId(appointment.getId());
        requestDto.setDoctorId(appointment.getDoctorId());
        requestDto.setPatientId(appointment.getPatientId());
        requestDto.setAmount(appointment.getAmount());
        requestDto.setServiceDate(appointment.getServiceDate());
        requestDto.setServiceType(appointment.getServiceType());
        requestDto.setPaymentStatus(appointment.isPaymentStatus());
        requestDto.setServiceDateEnd(appointment.getServiceDateEnd());
        requestDto.setPaymentType(appointment.getPaymentType());
        return requestDto;
    }
}
