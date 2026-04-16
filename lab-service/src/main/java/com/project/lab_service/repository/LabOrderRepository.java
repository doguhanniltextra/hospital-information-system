package com.project.lab_service.repository;

import com.project.lab_service.model.LabOrder;
import com.project.lab_service.model.LabOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LabOrderRepository extends JpaRepository<LabOrder, UUID> {
    List<LabOrder> findByPatientIdOrderByPriorityDescRequestedAtAsc(UUID patientId);
    List<LabOrder> findByStatusOrderByPriorityDescRequestedAtAsc(LabOrderStatus status);
    List<LabOrder> findByPatientIdAndStatusOrderByPriorityDescRequestedAtAsc(UUID patientId, LabOrderStatus status);
    List<LabOrder> findAllByOrderByPriorityDescRequestedAtAsc();
}
