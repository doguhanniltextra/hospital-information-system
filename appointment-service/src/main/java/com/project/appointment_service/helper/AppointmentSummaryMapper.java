package com.project.appointment_service.helper;

import com.project.appointment_service.dto.response.AppointmentSummaryDto;
import com.project.appointment_service.model.AppointmentSummary;

public class AppointmentSummaryMapper {
    public static AppointmentSummaryDto toDto(AppointmentSummary entity) {
        AppointmentSummaryDto dto = new AppointmentSummaryDto();
        dto.setId(entity.getId());
        dto.setPatientId(entity.getPatientId());
        dto.setPatientName(entity.getPatientName());
        dto.setPatientEmail(entity.getPatientEmail());
        dto.setDoctorId(entity.getDoctorId());
        dto.setDoctorName(entity.getDoctorName());
        dto.setDoctorSpecialization(entity.getDoctorSpecialization());
        dto.setAppointmentTime(entity.getAppointmentTime());
        dto.setServiceType(entity.getServiceType());
        dto.setAmount(entity.getAmount());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}
