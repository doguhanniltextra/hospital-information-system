package com.project.appointment_service.repository;

import com.project.appointment_service.model.DoctorLock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DoctorLockRepository extends JpaRepository<DoctorLock, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DoctorLock d WHERE d.doctorId = :doctorId")
    Optional<DoctorLock> findByIdForUpdate(@Param("doctorId") UUID doctorId);

    @Modifying
    @Query(value = "INSERT INTO doctor_locks (doctor_id) VALUES (:doctorId) ON CONFLICT (doctor_id) DO NOTHING", nativeQuery = true)
    void insertIfNotExists(@Param("doctorId") UUID doctorId);
}
