package com.project.appointment_service.config;

import com.project.grpc.DoctorServiceGrpc;
import com.project.patient_service.grpc.PatientQueryServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfig {

    @Bean
    public ManagedChannel patientServiceChannel() {
        return ManagedChannelBuilder
            .forAddress("patient-service", 9090)
            .usePlaintext() // For development - use TLS in production
            .build();
    }

    @Bean
    public ManagedChannel doctorServiceChannel() {
        return ManagedChannelBuilder
            .forAddress("doctor-service", 9090)
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