package com.project.dto.request;

import com.project.model.Specialization;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class CreateDoctorControllerRequestDto {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotNull
    private String name;

    @NotNull
    @Email
    @Column(unique = true)
    private String email;

    private String number;

    private Specialization specialization;

    @NotNull
    private int yearsOfExperience;

    @NotNull
    private String hospitalName;

    @NotNull
    private String department;

    @NotNull
    private int licenseNumber;

    @NotNull
    private boolean available;

    @NotNull
    private int patientCount;

    private boolean getMaximumPatient;

    private int maxPatientCount;

    @Version
    private int version;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public @NotNull String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    public @NotNull @Email String getEmail() {
        return email;
    }

    public void setEmail(@NotNull @Email String email) {
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

    @NotNull
    public int getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(@NotNull int yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public @NotNull String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(@NotNull String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public @NotNull String getDepartment() {
        return department;
    }

    public void setDepartment(@NotNull String department) {
        this.department = department;
    }

    @NotNull
    public int getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(@NotNull int licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    @NotNull
    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(@NotNull boolean available) {
        this.available = available;
    }

    @NotNull
    public int getPatientCount() {
        return patientCount;
    }

    public void setPatientCount(@NotNull int patientCount) {
        this.patientCount = patientCount;
    }

    public boolean isGetMaximumPatient() {
        return getMaximumPatient;
    }

    public int getMaxPatientCount() {
        return maxPatientCount;
    }

    public void setMaxPatientCount(int maxPatientCount) {
        this.maxPatientCount = maxPatientCount;
    }

    @NotNull
    private UUID authUserId;

    public UUID getAuthUserId() {
        return authUserId;
    }

    public void setAuthUserId(UUID authUserId) {
        this.authUserId = authUserId;
    }
}
