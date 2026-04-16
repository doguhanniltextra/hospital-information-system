package com.project.appointment_service.dto;

public class PatientInfoDTO {
    private String id;
    private String name;
    private String address;
    private String email;
    private InsuranceInfoDTO insuranceInfo;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public InsuranceInfoDTO getInsuranceInfo() {
        return insuranceInfo;
    }

    public void setInsuranceInfo(InsuranceInfoDTO insuranceInfo) {
        this.insuranceInfo = insuranceInfo;
    }
}
