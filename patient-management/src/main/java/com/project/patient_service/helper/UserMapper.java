package com.project.patient_service.helper;

import com.project.patient_service.dto.InsuranceInfoDto;
import com.project.patient_service.dto.request.*;
import com.project.patient_service.dto.response.*;
import com.project.patient_service.model.InsuranceInfo;
import com.project.patient_service.model.Patient;
import com.project.patient_service.readmodel.PatientSummary;
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class UserMapper {
    private static final Logger log = LoggerFactory.getLogger(UserMapper.class);


    public CreatePatientServiceResponseDto getCreatePatientServiceResponseDto(Patient patient) {
        CreatePatientServiceResponseDto createPatientServiceResponseDto = new CreatePatientServiceResponseDto();
        createPatientServiceResponseDto.setId(String.valueOf(patient.getId()));
        createPatientServiceResponseDto.setName(patient.getName());
        createPatientServiceResponseDto.setEmail(patient.getEmail());
        createPatientServiceResponseDto.setPhoneNumber(patient.getPhoneNumber());
        createPatientServiceResponseDto.setAddress(patient.getAddress());
        createPatientServiceResponseDto.setDateOfBirth(patient.getDateOfBirth());
        createPatientServiceResponseDto.setInsuranceInfo(toInsuranceInfoDto(patient.getInsuranceInfo()));
        return createPatientServiceResponseDto;
    }
    public KafkaPatientRequestDto getKafkaPatientRequestDto(Patient newPatient) {
        KafkaPatientRequestDto kafkaPatientRequestDto = new KafkaPatientRequestDto();
        kafkaPatientRequestDto.setId(newPatient.getId());
        kafkaPatientRequestDto.setEmail(newPatient.getEmail());
        kafkaPatientRequestDto.setPhoneNumber(newPatient.getPhoneNumber());
        kafkaPatientRequestDto.setName(newPatient.getName());
        return kafkaPatientRequestDto;
    }
    public  UpdatePatientServiceResponseDto getUpdatePatientServiceResponseDto(Patient updatedPatient) {
        UpdatePatientServiceResponseDto updatePatientServiceResponseDto = new UpdatePatientServiceResponseDto();
        updatePatientServiceResponseDto.setName(updatedPatient.getName());
        updatePatientServiceResponseDto.setAddress(updatedPatient.getAddress());
        updatePatientServiceResponseDto.setDateOfBirth(updatedPatient.getDateOfBirth());
        updatePatientServiceResponseDto.setEmail(updatedPatient.getEmail());
        updatePatientServiceResponseDto.setPhoneNumber(updatedPatient.getPhoneNumber());
        updatePatientServiceResponseDto.setInsuranceInfo(toInsuranceInfoDto(updatedPatient.getInsuranceInfo()));
        return updatePatientServiceResponseDto;
    }
    public  void getUpdatePatientRequestDto(UpdatePatientServiceRequestDto updatePatientServiceRequestDto, Patient patient) {
        patient.setName(updatePatientServiceRequestDto.getName());
        patient.setAddress(updatePatientServiceRequestDto.getAddress());
        patient.setEmail(updatePatientServiceRequestDto.getEmail());
        patient.setPhoneNumber(updatePatientServiceRequestDto.getPhoneNumber());
        patient.setDateOfBirth(LocalDate.parse(updatePatientServiceRequestDto.getDateOfBirth()));
        patient.setInsuranceInfo(toInsuranceInfo(updatePatientServiceRequestDto.getInsuranceInfo()));
    }
    // Single-object mapper for Page.map() — used by paginated endpoints
    public GetPatientServiceResponseDto toServiceResponseDto(Patient patient) {
        GetPatientServiceResponseDto dto = new GetPatientServiceResponseDto();
        dto.setId(patient.getId());
        dto.setEmail(patient.getEmail());
        dto.setPhoneNumber(patient.getPhoneNumber());
        dto.setAddress(patient.getAddress());
        dto.setDateOfBirth(patient.getDateOfBirth());
        dto.setName(patient.getName());
        dto.setInsuranceInfo(toInsuranceInfoDto(patient.getInsuranceInfo()));
        return dto;
    }

    public GetPatientServiceResponseDto toServiceResponseDtoFromSummary(PatientSummary summary) {
        if (summary == null) return null;
        GetPatientServiceResponseDto dto = new GetPatientServiceResponseDto();
        dto.setId(summary.getId());
        dto.setEmail(summary.getEmail());
        dto.setPhoneNumber(summary.getPhoneNumber());
        dto.setName(summary.getName());
        
        InsuranceInfoDto insurance = new InsuranceInfoDto();
        insurance.setProviderName(summary.getInsuranceProviderName());
        insurance.setPolicyNumber(summary.getInsurancePolicyNumber());
        dto.setInsuranceInfo(insurance);
        
        return dto;
    }

    // Single-object mapper for Page.map() — used by paginated endpoints
    public GetPatientControllerResponseDto toControllerResponseDto(GetPatientServiceResponseDto patient) {
        GetPatientControllerResponseDto dto = new GetPatientControllerResponseDto();
        dto.setAddress(patient.getAddress());
        dto.setId(String.valueOf(patient.getId()));
        dto.setName(patient.getName());
        dto.setEmail(patient.getEmail());
        dto.setPhoneNumber(patient.getPhoneNumber());
        dto.setDateOfBirth(patient.getDateOfBirth());
        dto.setInsuranceInfo(patient.getInsuranceInfo());
        return dto;
    }

    public  List<GetPatientServiceResponseDto> getGetPatientServiceResponseDtos(List<Patient> patients) {
        List<GetPatientServiceResponseDto> result = patients
                .stream()
                .map(this::toServiceResponseDto).toList();
        return result;
    }
    public  CreatePatientServiceRequestDto getCreatePatientServiceRequestDto(CreatePatientControllerRequestDto createPatientControllerRequestDto) {
        CreatePatientServiceRequestDto createPatientServiceRequestDto = new CreatePatientServiceRequestDto();
        createPatientServiceRequestDto.setAddress(createPatientControllerRequestDto.getAddress());
        createPatientServiceRequestDto.setEmail(createPatientControllerRequestDto.getEmail());
        createPatientServiceRequestDto.setPhoneNumber(createPatientControllerRequestDto.getPhoneNumber());
        createPatientServiceRequestDto.setName(createPatientControllerRequestDto.getName());
        createPatientServiceRequestDto.setRegisteredDate(createPatientControllerRequestDto.getRegisteredDate());
        createPatientServiceRequestDto.setDateOfBirth(createPatientControllerRequestDto.getDateOfBirth());
        createPatientServiceRequestDto.setInsuranceInfo(createPatientControllerRequestDto.getInsuranceInfo());
        return createPatientServiceRequestDto;
    }
    public  List<GetPatientControllerResponseDto> getGetPatientControllerResponseDtos(List<GetPatientServiceResponseDto> patients) {
        List<GetPatientControllerResponseDto> result = patients
                .stream()
                .map(patient -> {
                    GetPatientControllerResponseDto getPatientControllerResponseDto1 = new GetPatientControllerResponseDto();
                    getPatientControllerResponseDto1.setAddress(patient.getAddress());
                    getPatientControllerResponseDto1.setId(String.valueOf(patient.getId()));
                    getPatientControllerResponseDto1.setName(patient.getName());
                    getPatientControllerResponseDto1.setEmail(patient.getEmail());
                    getPatientControllerResponseDto1.setPhoneNumber(patient.getPhoneNumber());
                    getPatientControllerResponseDto1.setDateOfBirth(patient.getDateOfBirth());
                    getPatientControllerResponseDto1.setInsuranceInfo(patient.getInsuranceInfo());
                    return getPatientControllerResponseDto1;
                }).toList();
        return result;
    }

    public  UpdatePatientControllerResponseDto getUpdatePatientControllerResponseDto(UpdatePatientServiceResponseDto updatePatient) {
        UpdatePatientControllerResponseDto updatePatientControllerResponseDto = new UpdatePatientControllerResponseDto();
        updatePatientControllerResponseDto.setName(updatePatient.getName());
        updatePatientControllerResponseDto.setAddress(updatePatient.getAddress());
        updatePatientControllerResponseDto.setDateOfBirth(updatePatient.getDateOfBirth());
        updatePatientControllerResponseDto.setEmail(updatePatient.getEmail());
        updatePatientControllerResponseDto.setPhoneNumber(updatePatient.getPhoneNumber());
        updatePatientControllerResponseDto.setInsuranceInfo(updatePatient.getInsuranceInfo());
        return updatePatientControllerResponseDto;
    }

    public  UpdatePatientServiceRequestDto getUpdatePatientServiceRequestDto(UpdatePatientControllerRequestDto updatePatientControllerRequestDto) {
        UpdatePatientServiceRequestDto updatePatientServiceRequestDto = new UpdatePatientServiceRequestDto();
        updatePatientServiceRequestDto.setName(updatePatientControllerRequestDto.getName());
        updatePatientServiceRequestDto.setAddress(updatePatientControllerRequestDto.getAddress());
        updatePatientServiceRequestDto.setDateOfBirth(updatePatientControllerRequestDto.getDateOfBirth());
        updatePatientServiceRequestDto.setEmail(updatePatientControllerRequestDto.getEmail());
        updatePatientServiceRequestDto.setPhoneNumber(updatePatientControllerRequestDto.getPhoneNumber());
        updatePatientServiceRequestDto.setInsuranceInfo(updatePatientControllerRequestDto.getInsuranceInfo());
        return updatePatientServiceRequestDto;
    }

    private InsuranceInfoDto toInsuranceInfoDto(InsuranceInfo insuranceInfo) {
        if (insuranceInfo == null) {
            return null;
        }
        InsuranceInfoDto dto = new InsuranceInfoDto();
        dto.setPolicyNumber(insuranceInfo.getPolicyNumber());
        dto.setProviderName(insuranceInfo.getProviderName());
        if (insuranceInfo.getProviderType() != null) {
            dto.setProviderType(insuranceInfo.getProviderType().name());
        }
        return dto;
    }

    private InsuranceInfo toInsuranceInfo(InsuranceInfoDto insuranceInfoDto) {
        if (insuranceInfoDto == null) {
            return null;
        }
        InsuranceInfo insuranceInfo = new InsuranceInfo();
        insuranceInfo.setProviderName(insuranceInfoDto.getProviderName());
        insuranceInfo.setPolicyNumber(insuranceInfoDto.getPolicyNumber());
        if (insuranceInfoDto.getProviderType() != null && !insuranceInfoDto.getProviderType().isBlank()) {
            insuranceInfo.setProviderType(com.project.patient_service.model.InsuranceProviderType.valueOf(insuranceInfoDto.getProviderType().toUpperCase()));
        }
        return insuranceInfo;
    }
}
