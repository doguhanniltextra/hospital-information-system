package com.project.notification_service.grpc;

import com.project.notification_service.dto.PatientContactInfo;
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
