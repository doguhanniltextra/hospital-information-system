package com.project.readmodel;

import com.project.event.*;
import com.project.model.Doctor;
import com.project.model.LeaveStatus;
import com.project.model.Shift;
import com.project.model.ShiftStatus;
import com.project.repository.LeaveAbsenceRepository;
import com.project.repository.ShiftRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class DoctorEventHandler {

    private final DoctorSummaryRepository doctorSummaryRepository;
    private final ShiftRepository shiftRepository;
    private final LeaveAbsenceRepository leaveAbsenceRepository;

    public DoctorEventHandler(DoctorSummaryRepository doctorSummaryRepository, 
                              ShiftRepository shiftRepository, 
                              LeaveAbsenceRepository leaveAbsenceRepository) {
        this.doctorSummaryRepository = doctorSummaryRepository;
        this.shiftRepository = shiftRepository;
        this.leaveAbsenceRepository = leaveAbsenceRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleDoctorCreated(DoctorCreatedEvent event) {
        Doctor doctor = event.doctor();
        DoctorSummary summary = mapToSummary(doctor);
        doctorSummaryRepository.save(summary);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleDoctorUpdated(DoctorUpdatedEvent event) {
        Doctor doctor = event.doctor();
        DoctorSummary summary = doctorSummaryRepository.findById(doctor.getId())
                .orElse(new DoctorSummary());
        
        updateSummaryFromDoctor(summary, doctor);
        doctorSummaryRepository.save(summary);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleDoctorDeleted(DoctorDeletedEvent event) {
        doctorSummaryRepository.deleteById(event.doctorId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleShiftChanged(DoctorShiftChangedEvent event) {
        updateAvailability(event.doctorId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleLeaveChanged(DoctorLeaveChangedEvent event) {
        updateAvailability(event.doctorId());
    }

    private void updateAvailability(UUID doctorId) {
        doctorSummaryRepository.findById(doctorId).ifPresent(summary -> {
            boolean currentlyAvailable = computeAvailability(doctorId);
            summary.setAvailable(currentlyAvailable);
            summary.setLastUpdated(LocalDateTime.now());
            doctorSummaryRepository.save(summary);
        });
    }

    private boolean computeAvailability(UUID doctorId) {
        LocalDateTime now = LocalDateTime.now();
        
        // 1. Check if on leave
        boolean onLeave = leaveAbsenceRepository.existsByDoctorIdAndStatusAndStartDateTimeLessThanEqualAndEndDateTimeGreaterThanEqual(
                doctorId, LeaveStatus.APPROVED, now, now);
        
        if (onLeave) return false;

        // 2. Check if in active shift
        List<Shift> activeShifts = shiftRepository.findByDoctorIdAndShiftDateAndStatus(
                doctorId, now.toLocalDate(), ShiftStatus.ACTIVE);
        
        return activeShifts.stream().anyMatch(shift -> 
                !now.toLocalTime().isBefore(shift.getStartTime()) && 
                !now.toLocalTime().isAfter(shift.getEndTime()));
    }

    private DoctorSummary mapToSummary(Doctor doctor) {
        DoctorSummary summary = new DoctorSummary();
        summary.setId(doctor.getId());
        updateSummaryFromDoctor(summary, doctor);
        return summary;
    }

    private void updateSummaryFromDoctor(DoctorSummary summary, Doctor doctor) {
        summary.setName(doctor.getName());
        summary.setEmail(doctor.getEmail());
        summary.setNumber(doctor.getNumber());
        summary.setSpecialization(doctor.getSpecialization());
        summary.setHospitalName(doctor.getHospitalName());
        summary.setDepartment(doctor.getDepartment());
        summary.setAvailable(doctor.isAvailable());
        summary.setPatientCount(doctor.getPatientCount());
        summary.setLicenseNumber(doctor.getLicenseNumber());
        summary.setLastUpdated(LocalDateTime.now());
    }
}
