package com.project.billing_service.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.billing_service.command.BillingCommandService;
import com.project.billing_service.constants.KafkaTopics;
import com.project.billing_service.constants.LogMessages;
import com.project.billing_service.dto.AppointmentDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class KafkaConsumer {
    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

    private final ObjectMapper objectMapper; // FIX: injected Spring bean (not new ObjectMapper())
    private final BillingCommandService billingCommandService;

    public KafkaConsumer(ObjectMapper objectMapper, BillingCommandService billingCommandService) {
        this.objectMapper = objectMapper;
        this.billingCommandService = billingCommandService;
    }

    @KafkaListener(
            topics = "${kafka.topics.appointment-payment-updated:" + KafkaTopics.APPOINTMENT_PAYMENT_UPDATES + "}",
            groupId = "${kafka.groups.appointment:" + KafkaTopics.APPOINTMENT_GROUP + "}")
    public void listen(String message) throws Exception {
        AppointmentDTO appointment = objectMapper.readValue(message, AppointmentDTO.class);
        log.info(LogMessages.LISTENER_RECEIVED_MESSAGE, appointment);
        billingCommandService.processPaymentUpdate(appointment);
        log.info(LogMessages.INVOICE_GENERATED, appointment.getPatientId());
    }

    @KafkaListener(
            topics = "${kafka.topics.lab-order-placed:" + KafkaTopics.LAB_ORDER_PLACED + "}",
            groupId = "${kafka.groups.billing-lab-order:" + KafkaTopics.LAB_ORDER_GROUP + "}")
    public void listenLabOrder(String message) throws Exception {
        JsonNode event = objectMapper.readTree(message);
        billingCommandService.createUnbilledLabCharge(
                UUID.fromString(event.get("patientId").asText()),
                UUID.fromString(event.get("orderId").asText()),
                new BigDecimal(event.get("orderTotal").asText()),
                "TRY"
        );
    }

    @KafkaListener(
            topics = "${kafka.topics.inventory-item-consumed:" + KafkaTopics.INVENTORY_ITEM_CONSUMED + "}",
            groupId = "billing-inventory-group")
    public void listenInventoryConsumption(String message) throws Exception {
        JsonNode event = objectMapper.readTree(message);
        billingCommandService.createUnbilledInventoryCharge(
                UUID.fromString(event.get("patientId").asText()),
                UUID.fromString(event.get("itemId").asText()),
                event.get("quantity").asInt(),
                new BigDecimal(event.get("unitPriceSnapshot").asText()),
                event.get("currency").asText(),
                UUID.fromString(event.get("eventId").asText())
        );
    }

    @KafkaListener(
            topics = "${kafka.topics.admission-bed-charge:" + KafkaTopics.ADMISSION_BED_CHARGE + "}",
            groupId = "billing-admission-group")
    public void listenBedCharge(String message) throws Exception {
        JsonNode event = objectMapper.readTree(message);
        billingCommandService.createUnbilledBedCharge(
                UUID.fromString(event.get("patientId").asText()),
                UUID.fromString(event.get("admissionId").asText()),
                new BigDecimal(event.get("amount").asText()),
                event.get("currency").asText(),
                UUID.fromString(event.get("eventId").asText())
        );
    }

    @KafkaListener(
            topics = "${kafka.topics.admission-discharged:" + KafkaTopics.ADMISSION_DISCHARGED + "}",
            groupId = "billing-admission-group")
    public void listenDischarge(String message) throws Exception {
        JsonNode event = objectMapper.readTree(message);
        billingCommandService.finalizeDischargeBilling(
                UUID.fromString(event.get("patientId").asText()),
                UUID.fromString(event.get("admissionId").asText()),
                UUID.fromString(event.get("doctorId").asText())
        );
    }
}
