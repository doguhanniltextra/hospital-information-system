package com.project.helper;

import com.project.dto.UpdateDoctorServiceRequestDto;
import com.project.dto.UpdateDoctorServiceResponseDto;
import com.project.dto.request.CreateDoctorControllerRequestDto;
import com.project.dto.request.CreateDoctorServiceRequestDto;
import com.project.dto.response.CreateDoctorServiceResponseDto;
import com.project.model.Doctor;
import com.project.model.Specialization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class DoctorMapperTest {

    private DoctorMapper doctorMapper;

    @BeforeEach
    public void setUp() {
        doctorMapper = new DoctorMapper();
    }

    @Test
    public void toEntity_ShouldMapCorrectly() {
        CreateDoctorServiceRequestDto request = new CreateDoctorServiceRequestDto();
        request.setId(UUID.randomUUID());
        request.setName("Dr. Smith");
        request.setEmail("smith@test.com");
        request.setSpecialization(Specialization.Cardiologist);
        request.setMaxPatientCount(15);
        request.setPatientCount(0);

        Doctor doctor = doctorMapper.toEntity(request);

        assertThat(doctor.getId()).isEqualTo(request.getId());
        assertThat(doctor.getName()).isEqualTo(request.getName());
        assertThat(doctor.getEmail()).isEqualTo(request.getEmail());
        assertThat(doctor.getSpecialization()).isEqualTo(request.getSpecialization());
        assertThat(doctor.getMaxPatientCount()).isEqualTo(15);
        assertThat(doctor.getPatientCount()).isEqualTo(0);
    }

    @Test
    public void toCreateDoctorServiceResponseDto_ShouldMapCorrectly() {
        Doctor doctor = new Doctor();
        doctor.setId(UUID.randomUUID());
        doctor.setName("Dr. Jones");
        doctor.setEmail("jones@test.com");
        doctor.setHospitalName("City Hospital");

        CreateDoctorServiceResponseDto response = doctorMapper.toCreateDoctorServiceResponseDto(doctor);

        assertThat(response.getId()).isEqualTo(doctor.getId());
        assertThat(response.getName()).isEqualTo(doctor.getName());
        assertThat(response.getEmail()).isEqualTo(doctor.getEmail());
        assertThat(response.getHospitalName()).isEqualTo(doctor.getHospitalName());
    }

    @Test
    public void getUpdateDoctorServiceResponseDto_ShouldMapCorrectly() {
        Doctor doctor = new Doctor();
        doctor.setId(UUID.randomUUID());
        doctor.setName("Dr. Brown");
        doctor.setMaxPatientCount(25);
        doctor.setPatientCount(5);

        UpdateDoctorServiceResponseDto response = doctorMapper.getUpdateDoctorServiceResponseDto(doctor);

        assertThat(response.getId()).isEqualTo(doctor.getId());
        assertThat(response.getName()).isEqualTo(doctor.getName());
        assertThat(response.getMaxPatientCount()).isEqualTo(25);
        assertThat(response.getPatientCount()).isEqualTo(5);
    }

    @Test
    public void getDoctorRequestDto_ShouldUpdateExistingEntity() {
        Doctor existingDoctor = new Doctor();
        existingDoctor.setName("Old Name");
        existingDoctor.setMaxPatientCount(10);

        UpdateDoctorServiceRequestDto request = new UpdateDoctorServiceRequestDto();
        request.setName("New Name");
        request.setMaxPatientCount(20);
        request.setEmail("new@test.com");

        Doctor updatedDoctor = doctorMapper.getDoctorRequestDto(request, Optional.of(existingDoctor));

        assertThat(updatedDoctor.getName()).isEqualTo("New Name");
        assertThat(updatedDoctor.getMaxPatientCount()).isEqualTo(20);
        assertThat(updatedDoctor.getEmail()).isEqualTo("new@test.com");
    }

    @Test
    public void getCreateDoctorServiceRequestDto_ShouldMapFromControllerRequest() {
        CreateDoctorControllerRequestDto controllerRequest = new CreateDoctorControllerRequestDto();
        controllerRequest.setId(UUID.randomUUID());
        controllerRequest.setName("Dr. White");
        controllerRequest.setMaxPatientCount(30);

        CreateDoctorServiceRequestDto serviceRequest = doctorMapper.getCreateDoctorServiceRequestDto(controllerRequest);

        assertThat(serviceRequest.getId()).isEqualTo(controllerRequest.getId());
        assertThat(serviceRequest.getName()).isEqualTo(controllerRequest.getName());
        assertThat(serviceRequest.getMaxPatientCount()).isEqualTo(30);
    }
}
