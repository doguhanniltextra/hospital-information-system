package com.project.support_service.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class LabResultCompletedEvent {
    public String eventId;
    public String eventVersion;
    public Instant occurredAt;
    public UUID orderId;
    public UUID patientId;
    public UUID doctorId;
    public List<ResultItem> results;
    public String reportPdfUrl;
    public Instant completedAt;
    public String correlationId;

    @Data
    public static class ResultItem {
        public String testCode;
        public String value;
        public String unit;
        public String referenceRange;
        public String abnormalFlag;
    }
}
