package com.project.appointment_service.repository;

import com.project.appointment_service.model.AppointmentOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentOutboxRepository extends JpaRepository<AppointmentOutboxEvent, UUID> {
    List<AppointmentOutboxEvent> findByStatusOrderByCreatedAtAsc(String status);
}
