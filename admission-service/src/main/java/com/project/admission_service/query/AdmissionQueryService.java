package com.project.admission_service.query;

import com.project.admission_service.model.AdmissionStatus;
import com.project.admission_service.readmodel.AdmissionSummary;
import com.project.admission_service.readmodel.AdmissionSummaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdmissionQueryService {

    private final AdmissionSummaryRepository admissionSummaryRepository;

    public AdmissionQueryService(AdmissionSummaryRepository admissionSummaryRepository) {
        this.admissionSummaryRepository = admissionSummaryRepository;
    }

    public List<AdmissionSummary> getActiveAdmissions() {
        return admissionSummaryRepository.findByStatus(AdmissionStatus.ACTIVE);
    }
}
