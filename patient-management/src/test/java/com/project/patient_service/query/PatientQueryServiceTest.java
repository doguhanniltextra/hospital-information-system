package com.project.patient_service.query;

import com.project.patient_service.dto.response.GetPatientServiceResponseDto;
import com.project.patient_service.helper.UserMapper;
import com.project.patient_service.readmodel.PatientSummary;
import com.project.patient_service.readmodel.PatientSummaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PatientQueryServiceTest {

    @Mock
    private PatientSummaryRepository patientSummaryRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private PatientQueryService patientQueryService;

    @Test
    public void findPatientById_ShouldReturnPatientSummary() {
        UUID id = UUID.randomUUID();
        GetPatientQuery query = new GetPatientQuery(id);
        PatientSummary summary = new PatientSummary();
        summary.setId(id);

        when(patientSummaryRepository.findById(id)).thenReturn(Optional.of(summary));

        Optional<PatientSummary> result = patientQueryService.findPatientById(query);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(id);
    }

    @Test
    public void getPatients_ShouldReturnPaginatedDtos() {
        Pageable pageable = PageRequest.of(0, 10);
        GetPatientsQuery query = new GetPatientsQuery(pageable);
        PatientSummary summary = new PatientSummary();
        Page<PatientSummary> page = new PageImpl<>(Collections.singletonList(summary));

        when(patientSummaryRepository.findAll(pageable)).thenReturn(page);
        when(userMapper.toServiceResponseDtoFromSummary(any())).thenReturn(new GetPatientServiceResponseDto());

        Page<GetPatientServiceResponseDto> result = patientQueryService.getPatients(query);

        assertThat(result.getContent()).hasSize(1);
    }
}
