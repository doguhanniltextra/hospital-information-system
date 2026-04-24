package com.project.dto;

import com.project.model.Specialization;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class UpdateDoctorControllerResponseDto {
    private UUID id;
    private String name;
    private String email;
    private String number;
    private Specialization specialization;
    private int yearsOfExperience;
    private int maxPatientCount;


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

    public int getMaxPatientCount() {
        return maxPatientCount;
    }

    public void setMaxPatientCount(int maxPatientCount) {
        this.maxPatientCount = maxPatientCount;
    }
}
