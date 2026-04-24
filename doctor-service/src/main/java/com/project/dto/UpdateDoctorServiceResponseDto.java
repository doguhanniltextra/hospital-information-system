package com.project.dto;

import com.project.model.Specialization;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class UpdateDoctorServiceResponseDto {

    private UUID id;
    private String name;
    private String email;
    private String number;
    private Specialization specialization;
    private int yearsOfExperience;
    private String hospitalName;
    private String department;
    private int licenseNumber;
    private boolean available;
    private int patientCount;
    private boolean getMaximumPatient;
    private int maxPatientCount;
    private int version;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Specialization getSpecialization() {
        return specialization;
    }

    public void setSpecialization(Specialization specialization) {
        this.specialization = specialization;
    }

    public int getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(int yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(int licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public int getPatientCount() {
        return patientCount;
    }

    public void setPatientCount(int patientCount) {
        this.patientCount = patientCount;
    }

    public boolean isGetMaximumPatient() {
        return getMaximumPatient;
    }

    public void setGetMaximumPatient(boolean getMaximumPatient) {
        this.getMaximumPatient = getMaximumPatient;
    }

    public int getMaxPatientCount() {
        return maxPatientCount;
    }

    public void setMaxPatientCount(int maxPatientCount) {
        this.maxPatientCount = maxPatientCount;
    }
}
