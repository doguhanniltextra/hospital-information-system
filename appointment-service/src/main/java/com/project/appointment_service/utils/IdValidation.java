package com.project.appointment_service.utils;

import com.project.grpc.DoctorServiceGrpc;
import com.project.grpc.FindDoctorRequest;
import com.project.grpc.CheckAvailabilityRequest;
import com.project.grpc.AvailabilityResponse;
import com.project.grpc.DoctorResponse;
import com.project.grpc.GetAvailableDoctorsRequest;
import com.project.grpc.AvailabilityPageResponse;
import com.project.patient_service.grpc.PatientQueryServiceGrpc;
import com.project.patient_service.grpc.FindPatientRequest;
import com.project.patient_service.grpc.PatientResponse;
import com.project.appointment_service.dto.PatientInfoDTO;
import com.project.appointment_service.dto.DoctorInfoDTO;
import com.project.appointment_service.dto.DoctorAvailabilityResponseDTO;
import com.project.appointment_service.model.ServiceType;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class IdValidation {

    private static final Logger log = LoggerFactory.getLogger(IdValidation.class);

    private final PatientQueryServiceGrpc.PatientQueryServiceBlockingStub patientQueryStub;
    private final DoctorServiceGrpc.DoctorServiceBlockingStub doctorServiceStub;

    public IdValidation(
        PatientQueryServiceGrpc.PatientQueryServiceBlockingStub patientStub,
        DoctorServiceGrpc.DoctorServiceBlockingStub doctorStub) {
        this.patientQueryStub = patientStub;
        this.doctorServiceStub = doctorStub;
    }

    public boolean checkPatientExists(UUID patientId) {
        log.info("Checking patient existence via gRPC: {}", patientId);
        try {
            FindPatientRequest request = FindPatientRequest.newBuilder()
                .setPatientId(patientId.toString())
                .build();
            com.project.patient_service.grpc.ExistsResponse response = patientQueryStub.existsById(request);
            return response.getExists();
        } catch (StatusRuntimeException e) {
            log.error("gRPC error checking patient existence", e);
            return false;
        }
    }

    public PatientInfoDTO fetchPatientInfo(UUID patientId) {
        log.info("Fetching patient info via gRPC: {}", patientId);
        try {
            FindPatientRequest request = FindPatientRequest.newBuilder()
                .setPatientId(patientId.toString())
                .build();
            com.project.patient_service.grpc.PatientResponse response = patientQueryStub.findById(request);

            PatientInfoDTO patientInfo = new PatientInfoDTO();
            patientInfo.setId(response.getId());
            patientInfo.setName(response.getName());
            patientInfo.setEmail(response.getEmail());
            patientInfo.setAddress(response.getAddress());
            return patientInfo;
        } catch (StatusRuntimeException e) {
            log.error("gRPC error fetching patient info", e);
            return null;
        }
    }

    public boolean checkDoctorExists(UUID doctorId) {
        log.info("Checking doctor existence via gRPC: {}", doctorId);
        try {
            FindDoctorRequest request = FindDoctorRequest.newBuilder()
                .setDoctorId(doctorId.toString())
                .build();
            com.project.grpc.ExistsResponse response = doctorServiceStub.existsById(request);
            return response.getExists();
        } catch (StatusRuntimeException e) {
            log.error("gRPC error checking doctor existence", e);
            return false;
        }
    }

    public DoctorInfoDTO fetchDoctorInfo(UUID doctorId) {
        log.info("Fetching doctor info via gRPC: {}", doctorId);
        try {
            FindDoctorRequest request = FindDoctorRequest.newBuilder()
                .setDoctorId(doctorId.toString())
                .build();
            com.project.grpc.DoctorResponse response = doctorServiceStub.findById(request);

            DoctorInfoDTO doctorInfo = new DoctorInfoDTO();
            doctorInfo.setId(response.getId());
            doctorInfo.setName(response.getName());
            doctorInfo.setEmail(response.getEmail());
            doctorInfo.setSpecialization(response.getSpecialization().name());
            return doctorInfo;
        } catch (StatusRuntimeException e) {
            log.error("gRPC error fetching doctor info", e);
            return null;
        }
    }

    public DoctorAvailabilityResponseDTO checkDoctorAvailability(UUID doctorId, String start, String end, ServiceType serviceType) {
        log.info("Checking doctor availability via gRPC: {} from {} to {}", doctorId, start, end);
        try {
            CheckAvailabilityRequest request = CheckAvailabilityRequest.newBuilder()
                .setDoctorId(doctorId.toString())
                .setStartTime(start)
                .setEndTime(end)
                .setServiceType(com.project.grpc.ServiceType.valueOf(serviceType.name()))
                .build();

            AvailabilityResponse response = doctorServiceStub.checkAvailability(request);
            
            DoctorAvailabilityResponseDTO dto = new DoctorAvailabilityResponseDTO();
            dto.setAvailable(response.getAvailable());
            dto.setReasonCode(response.getReason());
            return dto;
        } catch (StatusRuntimeException e) {
            log.error("gRPC error checking doctor availability", e);
            DoctorAvailabilityResponseDTO dto = new DoctorAvailabilityResponseDTO();
            dto.setAvailable(false);
            dto.setReasonCode("SERVICE_UNAVAILABLE");
            return dto;
        }
    }

    public void increaseDoctorPatientCount(UUID doctorId) {
        log.info("Increasing doctor patient count via gRPC: {}", doctorId);
        try {
            FindDoctorRequest request = FindDoctorRequest.newBuilder()
                .setDoctorId(doctorId.toString())
                .build();
            com.project.grpc.ExistsResponse response = doctorServiceStub.increasePatientCount(request);
        } catch (StatusRuntimeException e) {
            log.error("gRPC error increasing doctor patient count", e);
        }
    }

    public com.project.appointment_service.dto.DoctorAvailabilityPageResponseDTO getAvailableDoctorOptions(String start, String end, ServiceType serviceType, String specialization, int page, int size) {
        log.info("Getting available doctor options via gRPC: from {} to {}", start, end);
        try {
            GetAvailableDoctorsRequest request = GetAvailableDoctorsRequest.newBuilder()
                .setStartTime(start)
                .setEndTime(end)
                .setServiceType(com.project.grpc.ServiceType.valueOf(serviceType.name()))
                .setSpecialization(specialization != null ? specialization : "")
                .setPage(page)
                .setSize(size)
                .build();

            AvailabilityPageResponse response = doctorServiceStub.getAvailableDoctors(request);

            com.project.appointment_service.dto.DoctorAvailabilityPageResponseDTO dto = new com.project.appointment_service.dto.DoctorAvailabilityPageResponseDTO();
            dto.setTotalElements(response.getTotalElements());
            dto.setTotalPages(response.getTotalPages());
            
            java.util.List<com.project.appointment_service.dto.DoctorAvailabilitySummaryDTO> doctors = response.getDoctorsList().stream()
                .map(s -> {
                    com.project.appointment_service.dto.DoctorAvailabilitySummaryDTO entry = new com.project.appointment_service.dto.DoctorAvailabilitySummaryDTO();
                    entry.setId(UUID.fromString(s.getId()));
                    entry.setName(s.getName());
                    entry.setSpecialization(s.getSpecialization());
                    entry.setAvailable(s.getAvailable());
                    return entry;
                }).toList();
            
            dto.setDoctors(doctors);
            return dto;
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting available doctor options", e);
            return new com.project.appointment_service.dto.DoctorAvailabilityPageResponseDTO();
        }
    }
}
