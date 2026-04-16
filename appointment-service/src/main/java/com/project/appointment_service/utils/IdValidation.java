package com.project.appointment_service.utils;

import com.project.appointment_service.config.RestTemplateAddresses;
import com.project.appointment_service.constants.Endpoints;
import com.project.appointment_service.dto.DoctorAvailabilityPageResponseDTO;
import com.project.appointment_service.dto.DoctorAvailabilityResponseDTO;
import com.project.appointment_service.dto.PatientInfoDTO;
import com.project.appointment_service.model.ServiceType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;


@Component
public class IdValidation {

    private static final Logger log = LoggerFactory.getLogger(IdValidation.class);

    private final String PATIENT_SERVICE_URL;
    private final String DOCTOR_FIND_URL;
    private final String DOCTOR_AVAILABILITY_URL;
    private final String DOCTORS_BULK_AVAILABILITY_URL;
    private final RestTemplate restTemplate;

    @Value("${internal.service.token:pm-internal-token}")
    private String internalServiceToken;

    public IdValidation(RestTemplateAddresses restTemplateAddresses, RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.PATIENT_SERVICE_URL = restTemplateAddresses.getPATIENT_SERVICE_URL();
        this.DOCTOR_FIND_URL = restTemplateAddresses.getDOCTOR_FIND_URL();
        this.DOCTOR_AVAILABILITY_URL = restTemplateAddresses.getDOCTOR_AVAILABILITY_URL();
        this.DOCTORS_BULK_AVAILABILITY_URL = restTemplateAddresses.getDOCTORS_BULK_AVAILABILITY_URL();
    }

    @CircuitBreaker(name = "patientService", fallbackMethod = "patientFallback")
    @Retry(name = "patientService")
    public boolean checkPatientExists(UUID patientId) {
        return fetchPatientInfo(patientId) != null;
    }

    @CircuitBreaker(name = "patientService", fallbackMethod = "patientInfoFallback")
    @Retry(name = "patientService")
    public PatientInfoDTO fetchPatientInfo(UUID patientId) {
        ResponseEntity<PatientInfoDTO> response =
                restTemplate.getForEntity(PATIENT_SERVICE_URL + patientId, PatientInfoDTO.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        }
        return null;
    }

    @CircuitBreaker(name = "doctorService", fallbackMethod = "doctorFallback")
    @Retry(name = "doctorService")
    public boolean checkDoctorExists(UUID doctorId) {
        ResponseEntity<java.util.Map> response = restTemplate.exchange(
                DOCTOR_FIND_URL + doctorId,
                HttpMethod.GET,
                new HttpEntity<>(buildInternalHeaders()),
                java.util.Map.class
        );
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return true;
        }
        return false;
    }

    @CircuitBreaker(name = "doctorService", fallbackMethod = "doctorAvailabilityFallback")
    @Retry(name = "doctorService")
    public DoctorAvailabilityResponseDTO checkDoctorAvailability(UUID doctorId, String start, String end, ServiceType serviceType) {
        String url = UriComponentsBuilder.fromHttpUrl(DOCTOR_AVAILABILITY_URL + doctorId + Endpoints.DOCTOR_AVAILABILITY_BY_ID_SUFFIX)
                .queryParam("start", start)
                .queryParam("end", end)
                .queryParam("serviceType", serviceType.name())
                .toUriString();

        ResponseEntity<DoctorAvailabilityResponseDTO> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(buildInternalHeaders()),
                DoctorAvailabilityResponseDTO.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        }
        return new DoctorAvailabilityResponseDTO();
    }

    @CircuitBreaker(name = "doctorService", fallbackMethod = "doctorOptionsFallback")
    @Retry(name = "doctorService")
    public DoctorAvailabilityPageResponseDTO getAvailableDoctorOptions(String start, String end, ServiceType serviceType, String specialization, int page, int size) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(DOCTORS_BULK_AVAILABILITY_URL)
                .queryParam("start", start)
                .queryParam("end", end)
                .queryParam("serviceType", serviceType.name())
                .queryParam("page", page)
                .queryParam("size", size);

        if (specialization != null && !specialization.isBlank()) {
            builder.queryParam("specialization", specialization);
        }

        ResponseEntity<DoctorAvailabilityPageResponseDTO> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                new HttpEntity<>(buildInternalHeaders()),
                DoctorAvailabilityPageResponseDTO.class
        );
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        }
        return new DoctorAvailabilityPageResponseDTO();
    }

    @CircuitBreaker(name = "doctorService", fallbackMethod = "voidFallback")
    @Retry(name = "doctorService")
    public void increaseDoctorPatientCount(UUID doctorId) {
        restTemplate.exchange(
                DOCTOR_AVAILABILITY_URL + doctorId + Endpoints.DOCTOR_INCREASE_PATIENT_SUFFIX,
                HttpMethod.PUT,
                new HttpEntity<>(buildInternalHeaders()),
                Void.class
        );
    }

    // Fallback: circuit is open or all retries exhausted → fail clearly
    private boolean patientFallback(UUID patientId, Throwable t) {
        log.error("Circuit breaker OPEN for patient-service. Cannot verify patient {}. Cause: {}", patientId, t.getMessage());
        return false;
    }

    private PatientInfoDTO patientInfoFallback(UUID patientId, Throwable t) {
        log.error("Circuit breaker OPEN for patient-service. Cannot fetch patient {} details. Cause: {}", patientId, t.getMessage());
        if (t instanceof HttpClientErrorException.NotFound) {
            return null;
        }
        return null;
    }

    private boolean doctorFallback(UUID doctorId, Throwable t) {
        log.error("Circuit breaker OPEN for doctor-service. Cannot verify doctor {}. Cause: {}", doctorId, t.getMessage());
        if (t instanceof com.project.appointment_service.exception.CustomConflictException) {
            throw (com.project.appointment_service.exception.CustomConflictException) t;
        }
        return false;
    }

    private DoctorAvailabilityResponseDTO doctorAvailabilityFallback(UUID doctorId, String start, String end, ServiceType serviceType, Throwable t) {
        log.error("Circuit breaker OPEN for doctor-service. Cannot verify availability for doctor {}. Cause: {}", doctorId, t.getMessage());
        throw new com.project.appointment_service.exception.CustomConflictException("DOCTOR_SERVICE_UNAVAILABLE");
    }

    private DoctorAvailabilityPageResponseDTO doctorOptionsFallback(String start, String end, ServiceType serviceType, String specialization, int page, int size, Throwable t) {
        log.error("Circuit breaker OPEN for doctor-service. Cannot fetch doctor options. Cause: {}", t.getMessage());
        throw new com.project.appointment_service.exception.CustomConflictException("DOCTOR_SERVICE_UNAVAILABLE");
    }

    private void voidFallback(UUID doctorId, Throwable t) {
        log.error("Circuit breaker OPEN for doctor-service. Cannot increase patient count for doctor {}. Cause: {}", doctorId, t.getMessage());
        throw new com.project.appointment_service.exception.CustomConflictException("Failed to increase doctor patient count: " + t.getMessage());
    }

    private HttpHeaders buildInternalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(Endpoints.INTERNAL_AUTH_HEADER, internalServiceToken);
        return headers;
    }
}
