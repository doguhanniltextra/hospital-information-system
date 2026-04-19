package com.project.support_service.repository;

import com.project.support_service.model.lab.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TestResultRepository extends JpaRepository<TestResult, UUID> {
    List<TestResult> findByOrderId(UUID orderId);
}
