package com.project.appointment_service.saga;

import com.project.appointment_service.dto.DoctorInfoDTO;
import com.project.appointment_service.dto.PatientInfoDTO;
import com.project.appointment_service.dto.DoctorAvailabilityResponseDTO;
import com.project.appointment_service.dto.request.CreateAppointmentServiceRequestDto;
import com.project.appointment_service.dto.response.CreateAppointmentServiceResponseDto;
import com.project.appointment_service.exception.CustomConflictException;
import com.project.appointment_service.helper.AppointmentMapper;
import com.project.appointment_service.model.Appointment;
import com.project.appointment_service.model.AppointmentStatus;
import com.project.appointment_service.repository.AppointmentRepository;
import com.project.appointment_service.service.AppointmentPersistenceService;
import com.project.appointment_service.utils.IdValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CreateAppointmentSaga {
    private static final Logger log = LoggerFactory.getLogger(CreateAppointmentSaga.class);

    private final IdValidation idValidation;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;
    private final AppointmentPersistenceService persistenceService;

    public CreateAppointmentSaga(IdValidation idValidation,
                                 AppointmentRepository appointmentRepository,
                                 AppointmentMapper appointmentMapper,
                                 AppointmentPersistenceService persistenceService) {
        this.idValidation = idValidation;
        this.appointmentRepository = appointmentRepository;
        this.appointmentMapper = appointmentMapper;
        this.persistenceService = persistenceService;
    }

    public CreateAppointmentServiceResponseDto execute(CreateAppointmentServiceRequestDto request) {
        UUID patientId = request.getPatientId();
        UUID doctorId = request.getDoctorId();

        // Step 1 & 2: Fetch Patient & Doctor Info (gRPC - Outside Transaction)
        PatientInfoDTO patientInfo = idValidation.fetchPatientInfo(patientId);
        if (patientInfo == null) {
            throw new RuntimeException("Patient does not exist or info unavailable: " + patientId);
        }

        DoctorInfoDTO doctorInfo = idValidation.fetchDoctorInfo(doctorId);
        if (doctorInfo == null) {
            throw new RuntimeException("Doctor does not exist or info unavailable: " + doctorId);
        }

        // Step 3: CheckAvailability (gRPC - Outside Transaction)
        DoctorAvailabilityResponseDTO availability = idValidation.checkDoctorAvailability(
                doctorId, request.getServiceDate(), request.getServiceDateEnd(), request.getServiceType());
        
        if (!availability.isAvailable()) {
            throw new CustomConflictException("Doctor is not available: " + availability.getReasonCode());
        }

        // Step 3.5: Check Overlapping Appointments (Local DB)
        java.util.List<Appointment> overlaps = appointmentRepository.findOverlappingAppointments(
                doctorId, request.getServiceDate(), request.getServiceDateEnd());
        if (!overlaps.isEmpty()) {
            throw new CustomConflictException("Time slot overlaps with an existing appointment.");
        }

        // Step 4: SaveAppointment & Outbox (Local Transactional Write)
        Appointment appointment = persistenceService.persistAppointmentAndOutbox(request, patientInfo, doctorInfo);

        // Step 5: The "Finalize" Step
        // Since we use Outbox, the background publisher handles the Kafka part.
        // We consider the saga locally finished once the outbox is saved.
        // However, we start as PENDING and confirm after both are saved in Step 4.
        
        log.info("Saga finalized: Appointment {} persisted with outbox event", appointment.getId());

        CreateAppointmentServiceResponseDto response = appointmentMapper.getCreateAppointmentServiceResponseDto(request);
        response.setStatus(appointment.getStatus().name());
        return response;
    }

}
