package com.project.dto;

import com.project.model.Specialization;

public class UpdateDoctorServiceRequestDto {
    private String name;
    private Specialization specialization;
    private String email;
    private String hospitalName;
    private int licenseNumber;
    private int maxPatientCount;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Specialization getSpecialization() {
        return specialization;
    }

    public void setSpecialization(Specialization specialization) {
        this.specialization = specialization;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public int getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(int licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public int getMaxPatientCount() {
        return maxPatientCount;
    }

    public void setMaxPatientCount(int maxPatientCount) {
        this.maxPatientCount = maxPatientCount;
    }
}
