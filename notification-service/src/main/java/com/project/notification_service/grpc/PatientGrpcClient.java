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

/**
 * Client for communicating with the Patient Service via gRPC.
 * Fetches and caches patient contact information for notification enrichment.
 */
@Service
public class PatientGrpcClient {
    private static final Logger log = LoggerFactory.getLogger(PatientGrpcClient.class);

    @GrpcClient("patient-service")
    private PatientQueryServiceGrpc.PatientQueryServiceBlockingStub patientStub;

    /**
     * Retrieves patient contact information (email, phone, etc.) from the Patient Service.
     * Results are cached to minimize inter-service gRPC calls.
     * 
     * @param patientId The UUID of the patient to look up
     * @return PatientContactInfo containing delivery details, or null if lookup fails
     */
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

