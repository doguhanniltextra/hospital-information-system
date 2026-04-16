package com.project.patient_service.dto.response;

import com.project.patient_service.dto.InsuranceInfoDto;

import java.time.LocalDate;

public class UpdatePatientServiceResponseDto {
    private String id;
    private String name;
    private String address;
    private LocalDate dateOfBirth;
    private String email;
    private String phoneNumber;
    private InsuranceInfoDto insuranceInfo;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

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

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public InsuranceInfoDto getInsuranceInfo() {
        return insuranceInfo;
    }

    public void setInsuranceInfo(InsuranceInfoDto insuranceInfo) {
        this.insuranceInfo = insuranceInfo;
    }
}
