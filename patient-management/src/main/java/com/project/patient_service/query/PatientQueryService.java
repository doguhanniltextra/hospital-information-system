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

/**
 * Service class for handling patient query operations.
 * Implements the read side of the CQRS pattern using the PatientSummary read model.
 */
@Service
@Transactional(readOnly = true)
public class PatientQueryService {

    private final PatientSummaryRepository patientSummaryRepository;
    private final UserMapper userMapper;
    private static final Logger log = LoggerFactory.getLogger(PatientQueryService.class);

    /**
     * Initializes the query service with the read model repository.
     * 
     * @param patientSummaryRepository Repository for the patient summary read model
     * @param userMapper Helper for mapping between read models and DTOs
     */
    public PatientQueryService(PatientSummaryRepository patientSummaryRepository, UserMapper userMapper) {
        this.patientSummaryRepository = patientSummaryRepository;
        this.userMapper = userMapper;
    }

    /**
     * Retrieves a paginated list of patient summaries.
     * 
     * @param query The query containing pagination details
     * @return A page of patient service response DTOs
     */
    public Page<GetPatientServiceResponseDto> getPatients(GetPatientsQuery query) {
        log.info(LogMessages.SERVICE_FIND_BIR_LIST_TRIGGERED);
        Page<PatientSummary> summaries = patientSummaryRepository.findAll(query.pageable());
        return summaries.map(userMapper::toServiceResponseDtoFromSummary);
    }

    /**
     * Finds a single patient summary by its unique identifier.
     * 
     * @param query The query containing the patient ID
     * @return An Optional containing the patient summary if found
     */
    public Optional<PatientSummary> findPatientById(GetPatientQuery query) {
        log.info(LogMessages.SERVICE_FIND_BY_ID_TRIGGERED);
        return patientSummaryRepository.findById(query.id());
    }

    /**
     * Checks if a patient exists with the specified email address.
     * 
     * @param email The email address to look up
     * @return True if a patient with the email exists, false otherwise
     */
    public boolean findPatientByEmail(String email) {
        log.info(LogMessages.SERVICE_FIND_BY_EMAIL_TRIGGERED);
        return patientSummaryRepository.existsByEmail(email);
    }

    /**
     * Finds a patient summary linked to a specific auth-service user ID.
     * 
     * @param authUserId The UUID from the auth-service account
     * @return An Optional containing the linked patient summary
     */
    public Optional<PatientSummary> findPatientByAuthUserId(java.util.UUID authUserId) {
        return patientSummaryRepository.findByAuthUserId(authUserId);
    }
}

