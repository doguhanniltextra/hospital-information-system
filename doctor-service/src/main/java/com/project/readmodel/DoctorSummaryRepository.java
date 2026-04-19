package com.project.readmodel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface DoctorSummaryRepository extends JpaRepository<DoctorSummary, UUID> {
}
