package com.project.patient_service.grpc;

import com.project.patient_service.model.Patient;
import com.project.patient_service.repository.PatientRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import java.util.UUID;

/**
 * gRPC service implementation for querying patient data.
 * Provides high-performance, cross-service communication for patient lookups.
 */
@GrpcService
public class PatientQueryServiceGrpcImpl extends PatientQueryServiceGrpc.PatientQueryServiceImplBase {

    private final PatientRepository patientRepository;

    /**
     * Initializes the gRPC service with the patient repository.
     * 
     * @param patientRepository Repository for accessing core patient data
     */
    public PatientQueryServiceGrpcImpl(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    /**
     * Finds a patient by ID and returns a gRPC response.
     * 
     * @param request The gRPC request containing the patient ID string
     * @param responseObserver The observer for sending the gRPC response
     */
    @Override
    public void findById(FindPatientRequest request, StreamObserver<PatientResponse> responseObserver) {
        try {
            UUID patientId = UUID.fromString(request.getPatientId());
            patientRepository.findById(patientId).ifPresentOrElse(
                patient -> {
                    PatientResponse response = PatientResponse.newBuilder()
                            .setId(patient.getId().toString())
                            .setName(patient.getName())
                            .setEmail(patient.getEmail())
                            .setPhoneNumber(patient.getPhoneNumber())
                            .setAddress(patient.getAddress())
                            .setDateOfBirth(patient.getDateOfBirth().toString())
                            .build();
                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                },
                () -> responseObserver.onError(new RuntimeException("Patient not found: " + patientId))
            );
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    /**
     * Checks if a patient exists by ID and returns a gRPC boolean response.
     * 
     * @param request The gRPC request containing the patient ID string
     * @param responseObserver The observer for sending the existence response
     */
    @Override
    public void existsById(FindPatientRequest request, StreamObserver<ExistsResponse> responseObserver) {
        try {
            UUID patientId = UUID.fromString(request.getPatientId());
            boolean exists = patientRepository.existsById(patientId);
            ExistsResponse response = ExistsResponse.newBuilder()
                    .setExists(exists)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
}

