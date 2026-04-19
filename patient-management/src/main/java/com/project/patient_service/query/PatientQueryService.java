package com.project.patient_service.query;

import com.project.patient_service.constants.LogMessages;
import com.project.patient_service.dto.response.GetPatientServiceResponseDto;
import com.project.patient_service.helper.UserMapper;
import com.project.patient_service.readmodel.PatientSummary;
import com.project.patient_service.readmodel.PatientSummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class PatientQueryService {

    private final PatientSummaryRepository patientSummaryRepository;
    private final UserMapper userMapper;
    private static final Logger log = LoggerFactory.getLogger(PatientQueryService.class);

    public PatientQueryService(PatientSummaryRepository patientSummaryRepository, UserMapper userMapper) {
        this.patientSummaryRepository = patientSummaryRepository;
        this.userMapper = userMapper;
    }

    public Page<GetPatientServiceResponseDto> getPatients(GetPatientsQuery query) {
        log.info(LogMessages.SERVICE_FIND_BIR_LIST_TRIGGERED);
        Page<PatientSummary> summaries = patientSummaryRepository.findAll(query.pageable());
        return summaries.map(userMapper::toServiceResponseDtoFromSummary);
    }

    public Optional<PatientSummary> findPatientById(GetPatientQuery query) {
        log.info(LogMessages.SERVICE_FIND_BY_ID_TRIGGERED);
        return patientSummaryRepository.findById(query.id());
    }

    public boolean findPatientByEmail(String email) {
        log.info(LogMessages.SERVICE_FIND_BY_EMAIL_TRIGGERED);
        return patientSummaryRepository.existsByEmail(email);
    }
}
