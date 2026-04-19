package com.project.support_service.repository;

import com.project.support_service.model.lab.LabOrder;
import com.project.support_service.model.lab.LabOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LabOrderRepository extends JpaRepository<LabOrder, UUID> {
    List<LabOrder> findByPatientIdOrderByPriorityDescRequestedAtAsc(UUID patientId);
    List<LabOrder> findByStatusOrderByPriorityDescRequestedAtAsc(LabOrderStatus status);
    List<LabOrder> findByPatientIdAndStatusOrderByPriorityDescRequestedAtAsc(UUID patientId, LabOrderStatus status);
    List<LabOrder> findAllByOrderByPriorityDescRequestedAtAsc();
}
