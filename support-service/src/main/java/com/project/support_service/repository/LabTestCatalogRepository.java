package com.project.support_service.repository;

import com.project.support_service.model.lab.LabTestCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabTestCatalogRepository extends JpaRepository<LabTestCatalog, String> {
}
