package com.project.patient_service.query;

import com.project.patient_service.constants.Endpoints;
import com.project.patient_service.constants.LogMessages;
import com.project.patient_service.constants.SwaggerMessages;
import com.project.patient_service.dto.response.GetPatientControllerResponseDto;
import com.project.patient_service.dto.response.GetPatientServiceResponseDto;
import com.project.patient_service.helper.UserMapper;
import com.project.patient_service.helper.UserValidator;
import com.project.patient_service.model.Patient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(Endpoints.PATIENT_CONTROLLER_REQUEST)
@Tag(name = "Patient Query Controller", description = "Query operations for Patients")
public class PatientQueryController {

    private static final Logger log = LoggerFactory.getLogger(PatientQueryController.class);
    private final PatientQueryService patientQueryService;
    private final UserMapper userMapper;
    private final UserValidator userValidator;

    public PatientQueryController(PatientQueryService patientQueryService, UserMapper userMapper,
            UserValidator userValidator) {
        this.patientQueryService = patientQueryService;
        this.userMapper = userMapper;
        this.userValidator = userValidator;
    }

    @GetMapping
    @Operation(summary = SwaggerMessages.GET_PATIENTS)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<Page<GetPatientControllerResponseDto>> getPatients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info(LogMessages.CONTROLLER_GET_TRIGGERED);

        int safeSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, safeSize);
        GetPatientsQuery query = new GetPatientsQuery(pageable);

        Page<GetPatientServiceResponseDto> patients = patientQueryService.getPatients(query);
        Page<GetPatientControllerResponseDto> result = patients.map(userMapper::toControllerResponseDto);

        return ResponseEntity.ok().body(result);
    }

    @GetMapping(Endpoints.PATIENT_CONTROLLER_FIND_PATIENT_BY_ID)
    @Operation(summary = SwaggerMessages.FIND_PATIENT_BY_ID)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'RECEPTIONIST') or @securityService.isPatientOwner(authentication, #id)")
    public ResponseEntity<com.project.patient_service.readmodel.PatientSummary> findPatientById(@PathVariable UUID id) {
        log.info(LogMessages.CONTROLLER_FIND_BY_ID_TRIGGERED);

        GetPatientQuery query = new GetPatientQuery(id);
        Optional<com.project.patient_service.readmodel.PatientSummary> currentId = patientQueryService
                .findPatientById(query);

        return currentId.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(Endpoints.PATIENT_CONTROLLER_FIND_PATIENT_BY_EMAIL)
    @Operation(summary = SwaggerMessages.FIND_PATIENT_BY_EMAIL)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<Boolean> findPatientByEmail(@PathVariable String email) {
        log.info(LogMessages.CONTROLLER_FIND_BY_EMAIL_TRIGGERED);

        // Note: Special lookup that doesn't use a Query object for now as it's a simple
        // boolean check
        boolean exists = patientQueryService.findPatientByEmail(email);

        return userValidator.getBooleanResponseEntity(exists);
    }
}
