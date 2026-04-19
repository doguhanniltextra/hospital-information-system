package com.project.appointment_service.dto;

import java.util.List;

public class DoctorAvailabilityPageResponseDTO {
    private List<DoctorAvailabilitySummaryDTO> doctors;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public List<DoctorAvailabilitySummaryDTO> getDoctors() {
        return doctors;
    }

    public void setDoctors(List<DoctorAvailabilitySummaryDTO> doctors) {
        this.doctors = doctors;
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

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
