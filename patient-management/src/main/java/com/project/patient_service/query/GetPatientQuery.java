package com.project.patient_service.query;

import java.util.UUID;

/**
 * Query to retrieve a single patient by ID.
 */
public record GetPatientQuery(UUID id) {
}
