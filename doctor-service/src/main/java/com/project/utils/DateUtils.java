package com.project.utils;

import com.project.exception.ApiException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (Exception ex) {
            throw new ApiException("INVALID_SLOT", "Invalid date format, expected yyyy-MM-dd", 400);
        }
    }

    public static LocalTime parseTime(String value) {
        try {
            return LocalTime.parse(value);
        } catch (Exception ex) {
            throw new ApiException("INVALID_SLOT", "Invalid time format, expected HH:mm", 400);
        }
    }

    public static LocalDateTime parseDateTime(String value) {
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (Exception ex) {
            throw new ApiException("INVALID_SLOT", "Invalid datetime format, expected yyyy-MM-dd HH:mm", 400);
        }
    }
}
