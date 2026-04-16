package com.project.appointment_service.config;

import com.project.appointment_service.constants.Endpoints;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RestTemplateAddresses {
    @Value("${patient.service.base-url:http://patient-management:8080}")
    private String patientServiceBaseUrl;

    @Value("${doctor.service.base-url:http://doctor-service:8083}")
    private String doctorServiceBaseUrl;

    public String getPATIENT_SERVICE_URL() {
        return patientServiceBaseUrl + Endpoints.PATIENT_FIND_BY_ID;
    }

    public String getDOCTOR_FIND_URL() {
        return doctorServiceBaseUrl + Endpoints.DOCTOR_FIND_BY_ID;
    }

    public String getDOCTOR_AVAILABILITY_URL() {
        return doctorServiceBaseUrl + Endpoints.DOCTOR_RESOURCE_ROOT;
    }

    public String getDOCTORS_BULK_AVAILABILITY_URL() {
        return doctorServiceBaseUrl + Endpoints.DOCTOR_BULK_AVAILABILITY;
    }
}
