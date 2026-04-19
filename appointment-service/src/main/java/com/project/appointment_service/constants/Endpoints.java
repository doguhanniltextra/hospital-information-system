package com.project.appointment_service.constants;

public class Endpoints {

    public static final String APPOINTMENT_CONTROLLER_REQUEST = "/appointments";

    public static final String CREATE_APPOINTMENT = "/";
    public static final String UPDATE_APPOINTMENT = "/{id}";
    public static final String UPDATE_APPOINTMENT_STATUS = "/appointment-update/{id}/{status}";
    public static final String DELETE_APPOINTMENT = "/{id}";
    public static final String GET_ALL_APPOINTMENTS = "/get";
    public static final String GET_ALL_APPOINTMENT_SUMMARIES = "/summary";
    public static final String VALIDATE_IDS = "/validate/{patientId}/{doctorId}";
    public static final String DOCTOR_OPTIONS = "/doctor-options";

    public static final String PATIENT_FIND_BY_ID = "/patients/find/";
    public static final String DOCTOR_FIND_BY_ID = "/doctors/find/";
    public static final String DOCTOR_RESOURCE_ROOT = "/doctors/";
    public static final String DOCTOR_AVAILABILITY_BY_ID_SUFFIX = "/availability";
    public static final String DOCTOR_BULK_AVAILABILITY = "/doctors/availability";
    public static final String DOCTOR_INCREASE_PATIENT_SUFFIX = "/increase-patient";

    public static final String INTERNAL_AUTH_HEADER = "X-Internal-Token";

}
