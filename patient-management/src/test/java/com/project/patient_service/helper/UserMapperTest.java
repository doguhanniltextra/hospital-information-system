package com.project.patient_service.helper;

import com.project.patient_service.dto.InsuranceInfoDto;
import com.project.patient_service.dto.request.CreatePatientControllerRequestDto;
import com.project.patient_service.dto.request.CreatePatientServiceRequestDto;
import com.project.patient_service.dto.request.KafkaPatientRequestDto;
import com.project.patient_service.dto.request.UpdatePatientServiceRequestDto;
import com.project.patient_service.dto.response.CreatePatientServiceResponseDto;
import com.project.patient_service.dto.response.UpdatePatientServiceResponseDto;
import com.project.patient_service.model.InsuranceInfo;
import com.project.patient_service.model.InsuranceProviderType;
import com.project.patient_service.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class UserMapperTest {

    private UserMapper userMapper;

    @BeforeEach
    public void setUp() {
        userMapper = new UserMapper();
    }

    @Test
    public void getCreatePatientServiceResponseDto_ShouldMapCorrectly() {
        Patient patient = new Patient();
        patient.setId(UUID.randomUUID());
        patient.setName("John Doe");
        patient.setEmail("john@test.com");
        patient.setPhoneNumber("123456789");
        patient.setAddress("123 Main St");
        patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
        
        InsuranceInfo insurance = new InsuranceInfo();
        insurance.setProviderName("Allianz");
        insurance.setPolicyNumber("POL123");
        insurance.setProviderType(InsuranceProviderType.PRIVATE);
        patient.setInsuranceInfo(insurance);

        CreatePatientServiceResponseDto response = userMapper.getCreatePatientServiceResponseDto(patient);

        assertThat(response.getId()).isEqualTo(patient.getId().toString());
        assertThat(response.getName()).isEqualTo(patient.getName());
        assertThat(response.getEmail()).isEqualTo(patient.getEmail());
        assertThat(response.getInsuranceInfo().getProviderName()).isEqualTo("Allianz");
        assertThat(response.getInsuranceInfo().getProviderType()).isEqualTo("PRIVATE");
    }

    @Test
    public void getKafkaPatientRequestDto_ShouldMapForOutbox() {
        Patient patient = new Patient();
        patient.setId(UUID.randomUUID());
        patient.setName("Jane Smith");
        patient.setEmail("jane@test.com");
        patient.setPhoneNumber("987654321");

        KafkaPatientRequestDto kafkaDto = userMapper.getKafkaPatientRequestDto(patient);

        assertThat(kafkaDto.getId()).isEqualTo(patient.getId());
        assertThat(kafkaDto.getName()).isEqualTo(patient.getName());
        assertThat(kafkaDto.getEmail()).isEqualTo(patient.getEmail());
    }

    @Test
    public void getUpdatePatientRequestDto_ShouldUpdateEntity() {
        Patient existingPatient = new Patient();
        existingPatient.setName("Old Name");

        UpdatePatientServiceRequestDto updateDto = new UpdatePatientServiceRequestDto();
        updateDto.setName("New Name");
        updateDto.setEmail("new@test.com");
        updateDto.setDateOfBirth("1985-05-20");
        updateDto.setAddress("New Address");
        updateDto.setPhoneNumber("555555");
        
        InsuranceInfoDto insuranceDto = new InsuranceInfoDto();
        insuranceDto.setProviderName("SGK");
        insuranceDto.setProviderType("SGK");
        updateDto.setInsuranceInfo(insuranceDto);

        userMapper.getUpdatePatientRequestDto(updateDto, existingPatient);

        assertThat(existingPatient.getName()).isEqualTo("New Name");
        assertThat(existingPatient.getEmail()).isEqualTo("new@test.com");
        assertThat(existingPatient.getDateOfBirth()).isEqualTo(LocalDate.of(1985, 5, 20));
        assertThat(existingPatient.getInsuranceInfo().getProviderName()).isEqualTo("SGK");
        assertThat(existingPatient.getInsuranceInfo().getProviderType()).isEqualTo(InsuranceProviderType.SGK);
    }

    @Test
    public void getCreatePatientServiceRequestDto_ShouldMapFromControllerRequest() {
        CreatePatientControllerRequestDto controllerRequest = new CreatePatientControllerRequestDto();
        controllerRequest.setName("Bob");
        controllerRequest.setEmail("bob@test.com");
        controllerRequest.setAddress("789 Oak Ave");

        CreatePatientServiceRequestDto serviceRequest = userMapper.getCreatePatientServiceRequestDto(controllerRequest);

        assertThat(serviceRequest.getName()).isEqualTo("Bob");
        assertThat(serviceRequest.getEmail()).isEqualTo("bob@test.com");
        assertThat(serviceRequest.getAddress()).isEqualTo("789 Oak Ave");
    }
}
