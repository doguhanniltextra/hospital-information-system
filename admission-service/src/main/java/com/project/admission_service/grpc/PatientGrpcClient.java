package com.project.admission_service.grpc;

import com.project.admission_service.dto.PatientContactInfo;
import com.project.admission_service.exception.ServiceUnavailableException;
import com.project.patient_service.grpc.FindPatientRequest;
import com.project.patient_service.grpc.PatientQueryServiceGrpc;
import com.project.patient_service.grpc.PatientResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PatientGrpcClient {
    private static final Logger log = LoggerFactory.getLogger(PatientGrpcClient.class);

    @GrpcClient("patient-service")
    private PatientQueryServiceGrpc.PatientQueryServiceBlockingStub patientStub;

    @Cacheable(value = "patientExistence", key = "#patientId")
    public boolean existsById(UUID patientId) {
        log.info("Checking patient existence via gRPC for patientId: {}", patientId);
        try {
            FindPatientRequest request = FindPatientRequest.newBuilder()
                    .setPatientId(patientId.toString())
                    .build();
            
            com.project.patient_service.grpc.ExistsResponse response = patientStub.existsById(request);
            return response.getExists();
        } catch (Exception e) {
            log.error("gRPC error while checking patient existence for {}: {}", patientId, e.getMessage());
            throw new ServiceUnavailableException("Patient Service is currently unreachable or returned an error.");
        }
    }

    @Cacheable(value = "patientContacts", key = "#patientId", unless = "#result == null")
    public PatientContactInfo getPatientContactInfo(UUID patientId) {
        log.info("Fetching patient contact info from gRPC for patientId: {}", patientId);
        try {
            FindPatientRequest request = FindPatientRequest.newBuilder()
                    .setPatientId(patientId.toString())
                    .build();
            
            PatientResponse response = patientStub.findById(request);
            
            return new PatientContactInfo(
                    response.getId(),
                    response.getName(),
                    response.getEmail(),
                    response.getPhoneNumber(),
                    response.getAddress()
            );
        } catch (Exception e) {
            log.error("Failed to fetch patient contact info for {}: {}", patientId, e.getMessage());
            return null;
        }
    }
}
