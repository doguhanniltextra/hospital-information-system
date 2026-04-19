package com.project.patient_service.service;

import com.project.patient_service.grpc.ExistsResponse;
import com.project.patient_service.grpc.FindPatientRequest;
import com.project.patient_service.grpc.PatientQueryServiceGrpc;
import com.project.patient_service.grpc.PatientResponse;
import com.project.patient_service.model.Patient;
import com.project.patient_service.repository.PatientRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@GrpcService
public class PatientGrpcService extends PatientQueryServiceGrpc.PatientQueryServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(PatientGrpcService.class);

    private final PatientRepository patientRepository;

    @Autowired
    public PatientGrpcService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    public void findById(FindPatientRequest request, StreamObserver<PatientResponse> responseObserver) {
        log.info("gRPC: findById called for patientId: {}", request.getPatientId());
        try {
            patientRepository.findById(UUID.fromString(request.getPatientId()))
                .ifPresentOrElse(
                    patient -> responseObserver.onNext(mapToResponse(patient)),
                    () -> responseObserver.onError(Status.NOT_FOUND.withDescription("Patient not found").asRuntimeException())
                );
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in findById", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void existsById(FindPatientRequest request, StreamObserver<ExistsResponse> responseObserver) {
        log.info("gRPC: existsById called for patientId: {}", request.getPatientId());
        try {
            boolean exists = patientRepository.existsById(UUID.fromString(request.getPatientId()));
            responseObserver.onNext(ExistsResponse.newBuilder().setExists(exists).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in existsById", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    private PatientResponse mapToResponse(Patient patient) {
        return PatientResponse.newBuilder()
            .setId(patient.getId().toString())
            .setName(patient.getName())
            .setEmail(patient.getEmail())
            .setPhoneNumber(patient.getPhoneNumber())
            .setAddress(patient.getAddress())
            .setDateOfBirth(patient.getDateOfBirth().toString())
            .build();
    }
}
