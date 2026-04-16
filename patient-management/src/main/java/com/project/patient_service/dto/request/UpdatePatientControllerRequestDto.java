package com.project.patient_service.dto.request;

import com.project.patient_service.dto.InsuranceInfoDto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UpdatePatientControllerRequestDto {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Date of birth is required")
    private String dateOfBirth;

    private InsuranceInfoDto insuranceInfo;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public InsuranceInfoDto getInsuranceInfo() {
        return insuranceInfo;
    }

    public void setInsuranceInfo(InsuranceInfoDto insuranceInfo) {
        this.insuranceInfo = insuranceInfo;
    }
}
