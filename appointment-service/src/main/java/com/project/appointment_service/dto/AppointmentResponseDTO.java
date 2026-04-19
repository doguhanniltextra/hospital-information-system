package com.project.appointment_service.dto;

import com.project.appointment_service.dto.response.CreateAppointmentServiceResponseDto;
import com.project.appointment_service.model.Appointment;
import org.springframework.stereotype.Component;

@Component
public class AppointmentResponseDTO {
    public AppointmentDTO toDTO(CreateAppointmentServiceResponseDto createAppointmentServiceResponseDto) {
        AppointmentDTO dto = new AppointmentDTO();
        dto.setAmount(createAppointmentServiceResponseDto.getAmount());
        dto.setPaymentStatus(createAppointmentServiceResponseDto.isPaymentStatus());
        dto.setServiceDate(createAppointmentServiceResponseDto.getServiceDate());
        dto.setServiceType(createAppointmentServiceResponseDto.getServiceType());
        dto.setStatus(createAppointmentServiceResponseDto.getStatus());
        return dto;
    }

    public AppointmentDTO toUpdateResponseDTO(Appointment updatedAppointment) {
        AppointmentDTO dto = new AppointmentDTO();
        dto.setAmount(updatedAppointment.getAmount());
        dto.setPaymentStatus(updatedAppointment.isPaymentStatus());
        dto.setServiceDate(updatedAppointment.getServiceDate());
        dto.setServiceType(updatedAppointment.getServiceType());
        dto.setStatus(updatedAppointment.getStatus() != null ? updatedAppointment.getStatus().name() : null);
        return dto;
    }
}
