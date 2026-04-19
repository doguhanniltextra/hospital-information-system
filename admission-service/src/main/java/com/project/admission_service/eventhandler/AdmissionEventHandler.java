package com.project.admission_service.eventhandler;

import com.project.admission_service.event.AdmissionCreatedEvent;
import com.project.admission_service.event.AdmissionUpdatedEvent;
import com.project.admission_service.model.Admission;
import com.project.admission_service.model.Bed;
import com.project.admission_service.model.Room;
import com.project.admission_service.model.Ward;
import com.project.admission_service.readmodel.AdmissionSummary;
import com.project.admission_service.readmodel.AdmissionSummaryRepository;
import com.project.admission_service.repository.BedRepository;
import com.project.admission_service.repository.RoomRepository;
import com.project.admission_service.repository.WardRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

@Component
public class AdmissionEventHandler {

    private final AdmissionSummaryRepository admissionSummaryRepository;
    private final BedRepository bedRepository;
    private final RoomRepository roomRepository;
    private final WardRepository wardRepository;

    public AdmissionEventHandler(AdmissionSummaryRepository admissionSummaryRepository,
                                 BedRepository bedRepository,
                                 RoomRepository roomRepository,
                                 WardRepository wardRepository) {
        this.admissionSummaryRepository = admissionSummaryRepository;
        this.bedRepository = bedRepository;
        this.roomRepository = roomRepository;
        this.wardRepository = wardRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAdmissionCreated(AdmissionCreatedEvent event) {
        upsertSummary(event.getAdmission());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAdmissionUpdated(AdmissionUpdatedEvent event) {
        upsertSummary(event.getAdmission());
    }

    private void upsertSummary(Admission admission) {
        AdmissionSummary summary = admissionSummaryRepository.findById(admission.getId())
                .orElse(new AdmissionSummary());

        summary.setId(admission.getId());
        summary.setPatientId(admission.getPatientId());
        summary.setDoctorId(admission.getDoctorId());
        summary.setBedId(admission.getBedId());
        summary.setAdmissionDate(admission.getAdmissionDate());
        summary.setDischargeDate(admission.getDischargeDate());
        summary.setStatus(admission.getStatus());

        Optional<Bed> bedOptional = bedRepository.findById(admission.getBedId());
        bedOptional.ifPresent(bed -> {
            summary.setBedNumber(bed.getBedNumber());
            summary.setRoomId(bed.getRoomId());
            Optional<Room> roomOptional = roomRepository.findById(bed.getRoomId());
            roomOptional.ifPresent(room -> {
                summary.setRoomNumber(room.getRoomNumber());
                summary.setWardId(room.getWardId());
                Optional<Ward> wardOptional = wardRepository.findById(room.getWardId());
                wardOptional.ifPresent(ward -> summary.setWardName(ward.getName()));
            });
        });

        admissionSummaryRepository.save(summary);
    }
}
