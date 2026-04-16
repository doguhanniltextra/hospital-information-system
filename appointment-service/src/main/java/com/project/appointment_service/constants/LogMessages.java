package com.project.appointment_service.constants;

public class LogMessages {

    // ---- CREATE ----
    public static final String SERVICE_CREATE_STARTING =
            "APPOINTMENT: SERVICE - CREATE - STARTING -> {}";
    public static final String SERVICE_CREATE_VALIDATE_PATIENT =
            "APPOINTMENT: SERVICE - CREATE - VALIDATION - PATIENT_ID -> {}";
    public static final String SERVICE_CREATE_VALIDATE_DOCTOR =
            "APPOINTMENT: SERVICE - CREATE - VALIDATION - DOCTOR_ID -> {}";
    public static final String SERVICE_CREATE_SAVING =
            "APPOINTMENT: SERVICE - CREATE - SAVE - TRIGGERED";

    // ---- UPDATE ----
    public static final String SERVICE_UPDATE_STARTING =
            "APPOINTMENT: SERVICE - UPDATE - STARTING -> {}";
    public static final String SERVICE_UPDATE_ENDED =
            "APPOINTMENT: SERVICE - UPDATE - ENDED";

    // ---- DELETE ----
    public static final String SERVICE_DELETE_TRIGGERED =
            "APPOINTMENT: SERVICE - DELETE - TRIGGERED -> {}";

    // ---- PAYMENT STATUS ----
    public static final String SERVICE_UPDATE_PAYMENT_STATUS_TRIGGERED =
            "APPOINTMENT: SERVICE - UPDATE_PAYMENT_STATUS - TRIGGERED -> {}";

    // ---- GET ALL ----
    public static final String SERVICE_GET_ALL_TRIGGERED =
            "APPOINTMENT: SERVICE - GET_ALL - TRIGGERED";

    // ---- KAFKA ----
    public static final String KAFKA_SEND_EVENT_TRIGGERED =
            "KAFKA: KAFKA - SEND_EVENT - TRIGGERED";
    // ---- KAFKA ----
    public static final String KAFKA_SEND_EVENT_ERROR =
            "KAFKA: KAFKA - SEND_EVENT - ERROR";
}
