package com.project.patient_service.grpc;

import com.project.patient_service.model.Patient;
import com.project.patient_service.repository.PatientRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import java.util.UUID;

@GrpcService
public class PatientQueryServiceGrpcImpl extends PatientQueryServiceGrpc.PatientQueryServiceImplBase {

    private final PatientRepository patientRepository;

    public PatientQueryServiceGrpcImpl(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

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
