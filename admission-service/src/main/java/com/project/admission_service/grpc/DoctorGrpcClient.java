package com.project.admission_service.grpc;

import com.project.grpc.DoctorServiceGrpc;
import com.project.grpc.FindDoctorRequest;
import com.project.grpc.ExistsResponse;
import com.project.admission_service.exception.ServiceUnavailableException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DoctorGrpcClient {
    private static final Logger log = LoggerFactory.getLogger(DoctorGrpcClient.class);

    @GrpcClient("doctor-service")
    private DoctorServiceGrpc.DoctorServiceBlockingStub doctorStub;

    @Cacheable(value = "doctorExistence", key = "#doctorId")
    public boolean existsById(UUID doctorId) {
        log.info("Checking doctor existence via gRPC for doctorId: {}", doctorId);
        try {
            FindDoctorRequest request = FindDoctorRequest.newBuilder()
                    .setDoctorId(doctorId.toString())
                    .build();
            
            ExistsResponse response = doctorStub.existsById(request);
            return response.getExists();
        } catch (Exception e) {
            log.error("gRPC error while checking doctor existence for {}: {}", doctorId, e.getMessage());
            throw new ServiceUnavailableException("Doctor Service is currently unreachable or returned an error.");
        }
    }
}
