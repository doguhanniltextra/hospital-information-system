package com.project.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class CreateLabOrderRequestDto {
    @NotNull
    private UUID patientId;
    @NotNull
    private List<LabTestRequest> tests;
    @NotNull
    private LocalDateTime requestedAt;
    @NotNull
    private String priority;

    public UUID getPatientId() { return patientId; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public List<LabTestRequest> getTests() { return tests; }
    public void setTests(List<LabTestRequest> tests) { this.tests = tests; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public static class LabTestRequest {
        public String testCode;
        public String priority;
        public String notes;
        public Double unitPrice;
        public Integer quantity;
    }
}
