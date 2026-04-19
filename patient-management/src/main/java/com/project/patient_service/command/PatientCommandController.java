package com.project.patient_service.command;

import com.project.patient_service.constants.Endpoints;
import com.project.patient_service.constants.LogMessages;
import com.project.patient_service.constants.SwaggerMessages;
import com.project.patient_service.dto.request.CreatePatientControllerRequestDto;
import com.project.patient_service.dto.request.CreatePatientServiceRequestDto;
import com.project.patient_service.dto.request.UpdatePatientControllerRequestDto;
import com.project.patient_service.dto.request.UpdatePatientServiceRequestDto;
import com.project.patient_service.dto.response.CreatePatientServiceResponseDto;
import com.project.patient_service.dto.response.UpdatePatientControllerResponseDto;
import com.project.patient_service.dto.response.UpdatePatientServiceResponseDto;
import com.project.patient_service.helper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(Endpoints.PATIENT_CONTROLLER_REQUEST)
@Tag(name = "Patient Command Controller", description = "Mutation operations for Patients")
public class PatientCommandController {

    private static final Logger log = LoggerFactory.getLogger(PatientCommandController.class);
    private final PatientCommandService patientCommandService;
    private final UserMapper userMapper;

    public PatientCommandController(PatientCommandService patientCommandService, UserMapper userMapper) {
        this.patientCommandService = patientCommandService;
        this.userMapper = userMapper;
    }

    @PostMapping
    @Operation(summary = SwaggerMessages.CREATE_PATIENT)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<CreatePatientServiceResponseDto> createPatient(
            @Valid @RequestBody CreatePatientControllerRequestDto requestDto) {
        log.info(LogMessages.CONTROLLER_CREATE_TRIGGERED);

        CreatePatientServiceRequestDto serviceRequestDto = userMapper.getCreatePatientServiceRequestDto(requestDto);
        CreatePatientCommand command = new CreatePatientCommand(serviceRequestDto);

        return ResponseEntity.ok().body(patientCommandService.createPatient(command));
    }

    @PutMapping(Endpoints.PATIENT_CONTROLLER_UPDATE_PATIENT)
    @Operation(summary = SwaggerMessages.UPDATE_PATIENT)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST') or @securityService.isPatientOwner(authentication, #id)")
    public ResponseEntity<UpdatePatientControllerResponseDto> updatePatient(@PathVariable UUID id,
            @Validated({ Default.class }) @RequestBody UpdatePatientControllerRequestDto requestDto) {
        log.info(LogMessages.CONTROLLER_UPDATE_TRIGGERED);

        UpdatePatientServiceRequestDto serviceRequestDto = userMapper.getUpdatePatientServiceRequestDto(requestDto);
        UpdatePatientCommand command = new UpdatePatientCommand(id, serviceRequestDto);

        UpdatePatientServiceResponseDto responseDto = patientCommandService.updatePatient(command);
        return ResponseEntity.ok().body(userMapper.getUpdatePatientControllerResponseDto(responseDto));
    }

    @DeleteMapping(Endpoints.PATIENT_CONTROLLER_DELETE_PATIENT)
    @Operation(summary = SwaggerMessages.DELETE_PATIENT)
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePatient(@PathVariable UUID id) {
        log.info(LogMessages.CONTROLLER_DELETE_TRIGGERED);

        DeletePatientCommand command = new DeletePatientCommand(id);
        patientCommandService.deletePatient(command);

        return ResponseEntity.noContent().build();
    }
}
