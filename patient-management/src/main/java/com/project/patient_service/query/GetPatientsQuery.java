package com.project.patient_service.query;

import org.springframework.data.domain.Pageable;

/**
 * Query to retrieve a paginated list of patients.
 */
public record GetPatientsQuery(Pageable pageable) {
}
