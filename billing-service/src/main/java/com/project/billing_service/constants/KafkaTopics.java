package com.project.billing_service.constants;

public class KafkaTopics {
    public static final String APPOINTMENT_PAYMENT_UPDATES = "appointment-payment-updates.v1";
    public static final String APPOINTMENT_GROUP = "appointment-group";

    // Topic Names (Spring property keys or defaults)
    public static final String LAB_ORDER_PLACED = "lab-order-placed.v1";
    public static final String LAB_ORDER_GROUP = "billing-lab-order-group";
    public static final String INVENTORY_ITEM_CONSUMED = "inventory-item-consumed.v1";
    public static final String ADMISSION_BED_CHARGE = "admission-bed-charge.v1";
    public static final String ADMISSION_DISCHARGED = "admission-discharged.v1";

    // Outbox publish topic
    public static final String BILLING_EVENTS = "billing-events.v1";
}
