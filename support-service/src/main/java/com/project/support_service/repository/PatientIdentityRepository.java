package com.project.support_service.repository;

import com.project.support_service.model.PatientIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientIdentityRepository extends JpaRepository<PatientIdentity, UUID> {
    Optional<PatientIdentity> findByAuthUserId(String authUserId);
}
