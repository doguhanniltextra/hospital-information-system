package com.project.billing_service.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.kafka.support.TopicPartitionOffset;
import com.project.billing_service.constants.KafkaTopics;

import org.springframework.beans.factory.annotation.Value;
@Configuration
public class KafkaErrorHandlerConfig {
    @Value("${kafka.topic.partitions:1}")
    private int partitions;

    @Value("${kafka.topic.replication-factor:1}")
    private int replicas;

    private static final Logger log = LoggerFactory.getLogger(KafkaErrorHandlerConfig.class);

    @Bean
    public DefaultErrorHandler errorHandler(KafkaOperations<Object, Object> template) {
        // 1. Define the recoverer (sends to originalTopic.DLQ)
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
            (r, e) -> {
                log.error("Recovering record from topic: {} to DLQ. Error: {}", r.topic(), e.getMessage());
                return new org.apache.kafka.common.TopicPartition(r.topic() + ".DLQ", -1);
            });

        // 2. Define the BackOff (Interval: 2000ms, MaxAttempts: 4 for 3 retries)
        FixedBackOff backOff = new FixedBackOff(2000L, 4L);

        // 3. Construct the handler
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        // 4. Log the failure before sending to DLQ
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            log.warn("Failed to process Kafka record. Attempt: {}, Error: {}", 
                deliveryAttempt, ex.getMessage());
        });

        return errorHandler;
    }

    @Bean
    public NewTopic appointmentPaymentDlq() {
        return TopicBuilder.name(KafkaTopics.APPOINTMENT_PAYMENT_UPDATES + ".DLQ")
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }

    @Bean
    public NewTopic labOrderPlacedDlq() {
        return TopicBuilder.name(KafkaTopics.LAB_ORDER_PLACED + ".DLQ")
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }

    @Bean
    public NewTopic inventoryItemConsumedDlq() {
        return TopicBuilder.name(KafkaTopics.INVENTORY_ITEM_CONSUMED + ".DLQ")
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }

    @Bean
    public NewTopic admissionBedChargeDlq() {
        return TopicBuilder.name(KafkaTopics.ADMISSION_BED_CHARGE + ".DLQ")
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }

    @Bean
    public NewTopic admissionDischargedDlq() {
        return TopicBuilder.name(KafkaTopics.ADMISSION_DISCHARGED + ".DLQ")
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }
}
