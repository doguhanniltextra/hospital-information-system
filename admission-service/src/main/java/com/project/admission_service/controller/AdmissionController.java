package com.project.admission_service.controller;

import com.project.admission_service.command.AdmissionCommandService;
import com.project.admission_service.constants.Endpoints;
import com.project.admission_service.dto.AdmissionRequest;
import com.project.admission_service.query.AdmissionQueryService;
import com.project.admission_service.readmodel.AdmissionSummary;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(Endpoints.ADMISSION_BASE)
public class AdmissionController {
    private final AdmissionCommandService admissionCommandService;
    private final AdmissionQueryService admissionQueryService;

    public AdmissionController(AdmissionCommandService admissionCommandService,
                               AdmissionQueryService admissionQueryService) {
        this.admissionCommandService = admissionCommandService;
        this.admissionQueryService = admissionQueryService;
    }

    @PostMapping(Endpoints.ADMIT)
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<com.project.admission_service.model.Admission> admitPatient(@RequestBody AdmissionRequest request) {
        return ResponseEntity.ok(admissionCommandService.admitPatient(request));
    }

    @PutMapping(Endpoints.DISCHARGE)
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN', 'DOCTOR')")
    public ResponseEntity<com.project.admission_service.model.Admission> dischargePatient(@PathVariable UUID id) {
        return ResponseEntity.ok(admissionCommandService.dischargePatient(id));
    }

    @GetMapping(Endpoints.LIST_ACTIVE)
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN', 'DOCTOR')")
    public ResponseEntity<List<AdmissionSummary>> getActiveAdmissions() {
        return ResponseEntity.ok(admissionQueryService.getActiveAdmissions());
    }
}
