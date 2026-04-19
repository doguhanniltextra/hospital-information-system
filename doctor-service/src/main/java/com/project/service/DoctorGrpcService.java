package com.project.service;

import com.project.command.DoctorCommandService;
import com.project.command.IncreasePatientNumberCommand;
import com.project.grpc.*;
import com.project.model.Doctor;
import com.project.model.ServiceType;
import com.project.query.DoctorQueryService;
import com.project.query.GetAvailableDoctorsQuery;
import com.project.query.GetDoctorQuery;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.UUID;

@GrpcService
public class DoctorGrpcService extends DoctorServiceGrpc.DoctorServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(DoctorGrpcService.class);

    private final DoctorQueryService doctorQueryService;
    private final DoctorCommandService doctorCommandService;

    @Autowired
    public DoctorGrpcService(DoctorQueryService doctorQueryService, DoctorCommandService doctorCommandService) {
        this.doctorQueryService = doctorQueryService;
        this.doctorCommandService = doctorCommandService;
    }

    @Override
    public void existsById(FindDoctorRequest request, StreamObserver<ExistsResponse> responseObserver) {
        log.info("gRPC: ExistsById called for doctorId: {}", request.getDoctorId());
        try {
            boolean exists = doctorQueryService
                .findDoctorById(new GetDoctorQuery(UUID.fromString(request.getDoctorId())))
                .isPresent();
            responseObserver.onNext(ExistsResponse.newBuilder().setExists(exists).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in ExistsById", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void findById(FindDoctorRequest request, StreamObserver<DoctorResponse> responseObserver) {
        log.info("gRPC: FindById called for doctorId: {}", request.getDoctorId());
        try {
            doctorQueryService.findDoctorById(new GetDoctorQuery(UUID.fromString(request.getDoctorId())))
                .ifPresentOrElse(
                    doctor -> {
                        responseObserver.onNext(mapToResponse(doctor));
                        responseObserver.onCompleted();
                    },
                    () -> responseObserver.onError(
                        Status.NOT_FOUND.withDescription("Doctor not found").asRuntimeException()
                    )
                );
        } catch (Exception e) {
            log.error("Error in FindById", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void checkAvailability(CheckAvailabilityRequest request, StreamObserver<AvailabilityResponse> responseObserver) {
        log.info("gRPC: CheckAvailability called for doctorId: {}", request.getDoctorId());
        try {
            GetAvailableDoctorsQuery query = new GetAvailableDoctorsQuery(
                request.getStartTime(),
                request.getEndTime(),
                ServiceType.valueOf(request.getServiceType().name())
            );
            com.project.dto.response.AvailabilityResponseDto result = doctorQueryService.checkDoctorAvailability(
                UUID.fromString(request.getDoctorId()),
                query
            );
            responseObserver.onNext(AvailabilityResponse.newBuilder()
                .setAvailable(result.isAvailable())
                .setReason(result.getReasonCode())
                .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in CheckAvailability", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void increasePatientCount(FindDoctorRequest request, StreamObserver<ExistsResponse> responseObserver) {
        log.info("gRPC: IncreasePatientCount called for doctorId: {}", request.getDoctorId());
        try {
            doctorCommandService.increasePatientNumber(
                new IncreasePatientNumberCommand(UUID.fromString(request.getDoctorId()))
            );
            responseObserver.onNext(ExistsResponse.newBuilder().setExists(true).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in IncreasePatientCount", e);
            responseObserver.onNext(ExistsResponse.newBuilder().setExists(false).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getAvailableDoctors(GetAvailableDoctorsRequest request, StreamObserver<AvailabilityPageResponse> responseObserver) {
        log.info("gRPC: GetAvailableDoctors called");
        try {
            GetAvailableDoctorsQuery query = new GetAvailableDoctorsQuery(
                request.getStartTime(),
                request.getEndTime(),
                ServiceType.valueOf(request.getServiceType().name())
            );
            Page<com.project.dto.response.DoctorAvailabilitySummaryDto> page = doctorQueryService.getAvailableDoctors(
                query,
                request.getSpecialization(),
                PageRequest.of(request.getPage(), request.getSize())
            );

            AvailabilityPageResponse.Builder builder = AvailabilityPageResponse.newBuilder()
                .setTotalElements(page.getTotalElements())
                .setTotalPages(page.getTotalPages());

            page.getContent().forEach(dto -> builder.addDoctors(
                DoctorAvailabilitySummary.newBuilder()
                    .setId(dto.getDoctorId().toString())   // ✅ getId() → getDoctorId()
                    .setName(dto.getName())
                    .setSpecialization(dto.getSpecialization() != null
                        ? dto.getSpecialization().name()
                        : "")
                    .setAvailable(dto.isAvailable())
                    .build()
            ));

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in GetAvailableDoctors", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    private DoctorResponse mapToResponse(Doctor doctor) {
        return DoctorResponse.newBuilder()
            .setId(doctor.getId().toString())
            .setName(doctor.getName())
            .setEmail(doctor.getEmail())
            .setNumber(doctor.getNumber() != null ? doctor.getNumber() : "")
            .setSpecialization(Specialization.valueOf(doctor.getSpecialization().name()))
            .setHospitalName(doctor.getHospitalName() != null ? doctor.getHospitalName() : "")
            .setDepartment(doctor.getDepartment() != null ? doctor.getDepartment() : "")
            .setLicenseNumber(doctor.getLicenseNumber())       // ✅ null kontrolü kaldırıldı
            .setYearsOfExperience(doctor.getYearsOfExperience()) // ✅ null kontrolü kaldırıldı
            .setPatientCount(doctor.getPatientCount())
            .setAvailable(doctor.isAvailable())
            .build();
    }
}