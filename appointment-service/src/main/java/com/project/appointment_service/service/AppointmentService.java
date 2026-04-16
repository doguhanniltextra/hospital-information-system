package com.project.appointment_service.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;


import com.project.appointment_service.constants.LogMessages;
import com.project.appointment_service.dto.AppointmentDTO;
import com.project.appointment_service.dto.AppointmentKafkaResponseDto;
import com.project.appointment_service.dto.DoctorAvailabilityResponseDTO;
import com.project.appointment_service.dto.PatientInfoDTO;
import com.project.appointment_service.dto.request.CreateAppointmentServiceRequestDto;
import com.project.appointment_service.dto.response.CreateAppointmentServiceResponseDto;
import com.project.appointment_service.exception.CustomNotFoundException;
import com.project.appointment_service.helper.AppointmentMapper;
import com.project.appointment_service.helper.AppointmentValidator;
import com.project.appointment_service.kafka.KafkaProducer;
import com.project.appointment_service.utils.IdValidation;

import jakarta.transaction.Transactional;
import org.apache.juli.logging.Log;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


import com.project.appointment_service.model.Appointment;
import com.project.appointment_service.repository.AppointmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class AppointmentService {

    private final KafkaProducer kafkaProducer;
    private final AppointmentRepository appointmentRepository;
    private final IdValidation idValidation;
    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final AppointmentMapper appointmentMapper;
    private final AppointmentValidator appointmentValidator;

    public AppointmentService(KafkaProducer kafkaProducer, AppointmentRepository appointmentRepository , IdValidation idValidation, AppointmentMapper appointmentMapper, AppointmentValidator appointmentValidator) {
        this.kafkaProducer = kafkaProducer;
        this.appointmentRepository = appointmentRepository;
        this.idValidation = idValidation;
        this.appointmentMapper = appointmentMapper;
        this.appointmentValidator = appointmentValidator;
    }

    public CreateAppointmentServiceResponseDto createAppointment(CreateAppointmentServiceRequestDto createAppointmentServiceRequestDto) {

        log.info(LogMessages.SERVICE_CREATE_SAVING, createAppointmentServiceRequestDto);
        UUID patientId = createAppointmentServiceRequestDto.getPatientId();
        UUID doctorId = createAppointmentServiceRequestDto.getDoctorId();

        AppointmentValidator.Result result = new AppointmentValidator.Result(patientId, doctorId);

        log.info(LogMessages.SERVICE_CREATE_VALIDATE_PATIENT, createAppointmentServiceRequestDto.getPatientId());
        appointmentValidator.checkPatientExistsOrNotForCreateAppointment(result.patientId());

        log.info(LogMessages.SERVICE_CREATE_VALIDATE_DOCTOR, createAppointmentServiceRequestDto.getDoctorId());
        appointmentValidator.checkDoctorExistsOrNotForCreateAppointment(result.doctorId());
        DoctorAvailabilityResponseDTO availabilityResponse = idValidation.checkDoctorAvailability(
                doctorId,
                createAppointmentServiceRequestDto.getServiceDate(),
                createAppointmentServiceRequestDto.getServiceDateEnd(),
                createAppointmentServiceRequestDto.getServiceType()
        );
        if (!availabilityResponse.isAvailable()) {
            throw new com.project.appointment_service.exception.CustomConflictException(
                    "Doctor is not available for booking. Reason: " + availabilityResponse.getReasonCode()
            );
        }

        java.util.List<Appointment> overlaps = appointmentRepository.findOverlappingAppointments(
                doctorId, 
                createAppointmentServiceRequestDto.getServiceDate(), 
                createAppointmentServiceRequestDto.getServiceDateEnd()
        );
        if (!overlaps.isEmpty()) {
            throw new com.project.appointment_service.exception.CustomConflictException("Time slot overlaps with an existing appointment.");
        }

        idValidation.increaseDoctorPatientCount(doctorId);

        Appointment appointment = appointmentMapper.getAppointment(createAppointmentServiceRequestDto);
        CreateAppointmentServiceResponseDto appointmentServiceResponseDto = appointmentMapper.getCreateAppointmentServiceResponseDto(createAppointmentServiceRequestDto);

        appointmentRepository.save(appointment);
        
        log.info("Mock Notification: Sent email/SMS confirmation to patient {} for appointment {}", patientId, appointment.getId());

        return appointmentServiceResponseDto;
    }

    public ResponseEntity<Appointment> updateAppointment(Appointment appointment) {
        log.info(LogMessages.SERVICE_UPDATE_STARTING, appointment.getId());
        Appointment existingAppointment = appointmentRepository.findById(appointment.getId())
                .orElseThrow(() -> new CustomNotFoundException("Appointment not found: " + appointment.getId()));
        appointmentMapper.updateAppointmentExtracted(appointment, existingAppointment);
        log.info(LogMessages.SERVICE_UPDATE_ENDED);
        return ResponseEntity.ok().body(appointmentRepository.save(existingAppointment));
    }

    public void deleteAppointment(UUID id) {
        log.info(LogMessages.SERVICE_DELETE_TRIGGERED, id);
        appointmentRepository.deleteById(id);
    }

    public void updatePaymentStatus(UUID id, boolean status) {
        log.info(LogMessages.SERVICE_UPDATE_PAYMENT_STATUS_TRIGGERED);

        Appointment appointment = appointmentValidator.getAppointmentForUpdatePaymentStatus(id, appointmentRepository);

        appointment.setPaymentStatus(status);
        appointmentRepository.save(appointment);

        PatientInfoDTO patientInfo = idValidation.fetchPatientInfo(appointment.getPatientId());
        AppointmentKafkaResponseDto appointmentDTO = appointmentValidator.getAppointmentKafkaResponseDto(status, appointment, patientInfo);
        appointmentValidator.updatePaymentStatusKafkaSendEvent(status, appointmentDTO, kafkaProducer);
    }

    public Page<Appointment> getAllAppointments(Pageable pageable) {
        log.info(LogMessages.SERVICE_GET_ALL_TRIGGERED);
        return appointmentRepository.findAll(pageable);
    }
    
}
