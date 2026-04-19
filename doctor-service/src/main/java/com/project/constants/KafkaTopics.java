package com.project.constants;

public class KafkaTopics {
    private KafkaTopics() {}

    public static final String LAB_ORDER_PLACED = "lab-order-placed.v1";
    public static final String LAB_RESULT_COMPLETED = "lab-result-completed.v1";
    public static final String DOCTOR_UPDATED = "doctor-updated.v1";
    public static final String DOCTOR_LAB_RESULT_GROUP = "doctor-lab-result-group";
}
