package com.project.appointment_service.dto;

import java.util.List;

public class DoctorAvailabilityPageResponseDTO {
    private List<DoctorAvailabilitySummaryDTO> content;
    private int page;
    private int size;
    private long totalElements;

    public List<DoctorAvailabilitySummaryDTO> getContent() {
        return content;
    }

    public void setContent(List<DoctorAvailabilitySummaryDTO> content) {
        this.content = content;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }
}
