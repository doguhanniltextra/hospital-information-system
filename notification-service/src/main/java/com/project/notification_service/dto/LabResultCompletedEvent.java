package com.project.notification_service.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class LabResultCompletedEvent {
    public String eventId;
    public UUID patientId;
    public UUID doctorId;
    public List<ResultItem> results;
    public String reportPdfUrl;

    public static class ResultItem {
        public String testCode;
        public String value;
        public String unit;
        public String abnormalFlag;
    }
}
