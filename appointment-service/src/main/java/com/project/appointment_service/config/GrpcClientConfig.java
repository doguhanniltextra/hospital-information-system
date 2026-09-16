package com.project.appointment_service.config;

import com.project.grpc.DoctorServiceGrpc;
import com.project.patient_service.grpc.PatientQueryServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfig {

    @Value("${patient.service.grpc.host:patient-management}")
    private String patientServiceHost;

    @Value("${patient.service.grpc.port:9090}")
    private int patientServicePort;

    @Value("${doctor.service.grpc.host:doctor-service}")
    private String doctorServiceHost;

    @Value("${doctor.service.grpc.port:9005}")
    private int doctorServicePort;

    @Bean
    public ManagedChannel patientServiceChannel() {
        return ManagedChannelBuilder
            .forAddress(patientServiceHost, patientServicePort)
            .usePlaintext() // For development - use TLS in production
            .build();
    }

    @Bean
    public ManagedChannel doctorServiceChannel() {
        return ManagedChannelBuilder
            .forAddress(doctorServiceHost, doctorServicePort)
            .usePlaintext() // For development - use TLS in production
            .build();
    }

@Bean
public PatientQueryServiceGrpc.PatientQueryServiceBlockingStub patientQueryStub(
        @Qualifier("patientServiceChannel") ManagedChannel channel) {
    return PatientQueryServiceGrpc.newBlockingStub(channel);
}

@Bean
public DoctorServiceGrpc.DoctorServiceBlockingStub doctorServiceStub(
        @Qualifier("doctorServiceChannel") ManagedChannel channel) {
    return DoctorServiceGrpc.newBlockingStub(channel);
}
}