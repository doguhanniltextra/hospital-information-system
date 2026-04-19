package com.project.support_service.query;

import com.project.support_service.model.lab.LabOrder;
import com.project.support_service.model.lab.LabOrderStatus;
import com.project.support_service.model.lab.LabTestCatalog;
import com.project.support_service.model.lab.TestResult;
import com.project.support_service.repository.LabOrderRepository;
import com.project.support_service.repository.LabTestCatalogRepository;
import com.project.support_service.repository.TestResultRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class LabQueryService {
    private final LabOrderRepository labOrderRepository;
    private final LabTestCatalogRepository catalogRepository;
    private final TestResultRepository testResultRepository;

    public LabQueryService(LabOrderRepository labOrderRepository,
                           LabTestCatalogRepository catalogRepository,
                           TestResultRepository testResultRepository) {
        this.labOrderRepository = labOrderRepository;
        this.catalogRepository = catalogRepository;
        this.testResultRepository = testResultRepository;
    }

    @Cacheable(value = "labCatalog")
    public List<LabTestCatalog> getCatalog() {
        return catalogRepository.findAll();
    }

    @Cacheable(value = "labOrders", key = "#patientId", condition = "#patientId != null")
    public List<LabOrder> getOrdersByPatient(UUID patientId) {
        return labOrderRepository.findByPatientIdOrderByPriorityDescRequestedAtAsc(patientId);
    }

    public List<LabOrder> getOrdersByStatus(LabOrderStatus status) {
        return labOrderRepository.findByStatusOrderByPriorityDescRequestedAtAsc(status);
    }

    public LabOrder getOrder(UUID orderId) {
        return labOrderRepository.findById(orderId).orElse(null);
    }

    public List<TestResult> getResults(UUID orderId) {
        return testResultRepository.findByOrderId(orderId);
    }
}
