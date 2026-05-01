package com.project.appointment_service.helper;

import com.project.appointment_service.model.Appointment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for AppointmentValidator's pure logic methods.
 * No Spring context or gRPC dependencies needed — all gRPC-backed methods
 * are covered indirectly via CreateAppointmentSagaTest.
 */
@DisplayName("AppointmentValidator")
class AppointmentValidatorTest {

    private AppointmentValidator validator;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @BeforeEach
    void setUp() {
        // IdValidation is the only dependency; we don't call gRPC methods in these tests
        validator = new AppointmentValidator(mock(com.project.appointment_service.utils.IdValidation.class));
    }

    // ── getAppointments() – past-date filter ──────────────────────────────────

    @Test
    @DisplayName("getAppointments() returns only appointments whose end date is in the past")
    void getAppointments_onlyReturnsPastAppointments() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);

        Appointment past = new Appointment();
        past.setServiceDateEnd("2026-05-01 10:00"); // before now

        Appointment future = new Appointment();
        future.setServiceDateEnd("2026-07-01 10:00"); // after now

        List<Appointment> result = validator.getAppointments(List.of(past, future), now, formatter);

        assertThat(result).containsExactly(past);
    }

    @Test
    @DisplayName("getAppointments() returns empty list when all appointments are in the future")
    void getAppointments_allFuture_returnsEmpty() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);

        Appointment future1 = new Appointment();
        future1.setServiceDateEnd("2026-12-01 09:00");
        Appointment future2 = new Appointment();
        future2.setServiceDateEnd("2027-01-01 09:00");

        List<Appointment> result = validator.getAppointments(List.of(future1, future2), now, formatter);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getAppointments() silently ignores appointments with unparseable date strings")
    void getAppointments_invalidDateFormat_isSkipped() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);

        Appointment invalid = new Appointment();
        invalid.setServiceDateEnd("not-a-date");

        Appointment past = new Appointment();
        past.setServiceDateEnd("2026-05-01 10:00");

        List<Appointment> result = validator.getAppointments(List.of(invalid, past), now, formatter);

        assertThat(result).containsExactly(past);
    }

    @Test
    @DisplayName("getAppointments() returns empty list when input is empty")
    void getAppointments_emptyInput_returnsEmpty() {
        List<Appointment> result = validator.getAppointments(
                List.of(), LocalDateTime.now(), formatter);

        assertThat(result).isEmpty();
    }

    // ── getLocalDateTime() ────────────────────────────────────────────────────

    @Test
    @DisplayName("getLocalDateTime() returns a value close to now")
    void getLocalDateTime_returnsCurrentTime() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        LocalDateTime result = validator.getLocalDateTime();
        LocalDateTime after  = LocalDateTime.now().plusSeconds(1);

        assertThat(result).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    }
}
