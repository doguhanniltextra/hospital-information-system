package com.project.query;

import com.project.dto.response.AvailabilityResponseDto;
import com.project.dto.response.ShiftResponseDto;
import com.project.exception.DoctorNotFoundException;
import com.project.helper.DoctorMapper;
import com.project.model.Doctor;
import com.project.model.Shift;
import com.project.model.ShiftStatus;
import com.project.repository.DoctorRepository;
import com.project.repository.LeaveAbsenceRepository;
import com.project.repository.ShiftRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DoctorQueryServiceTest {

    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private ShiftRepository shiftRepository;
    @Mock
    private LeaveAbsenceRepository leaveAbsenceRepository;
    @Mock
    private DoctorMapper doctorMapper;

    @InjectMocks
    private DoctorQueryService doctorQueryService;

    @Test
    public void findDoctorById_ShouldReturnDoctor() {
        UUID id = UUID.randomUUID();
        GetDoctorQuery query = new GetDoctorQuery(id);
        Doctor doctor = new Doctor();
        doctor.setId(id);

        when(doctorRepository.findById(id)).thenReturn(Optional.of(doctor));

        Optional<Doctor> result = doctorQueryService.findDoctorById(query);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(id);
    }

    @Test
    public void listShifts_ShouldReturnMappedDtos() {
        UUID doctorId = UUID.randomUUID();
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(7);
        Shift shift = new Shift();
        ShiftResponseDto dto = new ShiftResponseDto();

        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(new Doctor()));
        when(shiftRepository.findByDoctorIdAndShiftDateBetweenAndStatus(eq(doctorId), any(), any(), eq(ShiftStatus.ACTIVE)))
                .thenReturn(Collections.singletonList(shift));
        when(doctorMapper.toShiftResponseDto(shift)).thenReturn(dto);

        List<ShiftResponseDto> result = doctorQueryService.listShifts(doctorId, from, to);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(dto);
    }

    @Test
    public void listShifts_WhenDoctorNotFound_ShouldThrowException() {
        UUID doctorId = UUID.randomUUID();
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> doctorQueryService.listShifts(doctorId, LocalDate.now(), LocalDate.now()))
                .isInstanceOf(DoctorNotFoundException.class);
    }

    @Test
    public void checkDoctorAvailability_ShouldReturnAvailable_WhenShiftExists() {
        UUID doctorId = UUID.randomUUID();
        GetAvailableDoctorsQuery query = new GetAvailableDoctorsQuery("2026-05-24 10:00", "2026-05-24 11:00", null);
        Shift shift = new Shift();
        shift.setStartTime(LocalTime.of(9, 0));
        shift.setEndTime(LocalTime.of(17, 0));

        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(new Doctor()));
        when(shiftRepository.findByDoctorIdAndShiftDateAndStatus(eq(doctorId), any(), eq(ShiftStatus.ACTIVE)))
                .thenReturn(Collections.singletonList(shift));
        when(leaveAbsenceRepository.existsByDoctorIdAndStatusAndStartDateTimeLessThanEqualAndEndDateTimeGreaterThanEqual(any(), any(), any(), any()))
                .thenReturn(false);

        AvailabilityResponseDto result = doctorQueryService.checkDoctorAvailability(doctorId, query);

        assertThat(result.isAvailable()).isTrue();
        assertThat(result.getReasonCode()).isEqualTo("IN_SHIFT");
    }

    @Test
    public void checkDoctorAvailability_ShouldReturnUnavailable_WhenOutsideShift() {
        UUID doctorId = UUID.randomUUID();
        GetAvailableDoctorsQuery query = new GetAvailableDoctorsQuery("2026-05-24 18:00", "2026-05-24 19:00", null);
        Shift shift = new Shift();
        shift.setStartTime(LocalTime.of(9, 0));
        shift.setEndTime(LocalTime.of(17, 0));

        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(new Doctor()));
        when(shiftRepository.findByDoctorIdAndShiftDateAndStatus(eq(doctorId), any(), eq(ShiftStatus.ACTIVE)))
                .thenReturn(Collections.singletonList(shift));

        AvailabilityResponseDto result = doctorQueryService.checkDoctorAvailability(doctorId, query);

        assertThat(result.isAvailable()).isFalse();
        assertThat(result.getReasonCode()).isEqualTo("OUTSIDE_SHIFT");
    }
}
