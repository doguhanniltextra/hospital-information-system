package com.project.appointment_service.service;

import com.project.appointment_service.repository.AppointmentRepository;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for automated cleanup of old appointment records.
 * Maintains database health by removing historical data that is no longer clinically relevant.
 */
@Service
public class AppointmentCleanupService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentCleanupService.class);

    private final AppointmentRepository appointmentRepository;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Initializes the AppointmentCleanupService.
     * 
     * @param appointmentRepository Repository for Appointment operations
     */
    public AppointmentCleanupService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    /**
     * Scheduled task that runs daily at 6:00 AM.
     * Deletes all appointments that are already PAID and whose end date is in the past.
     * Uses a single optimized database query to avoid loading entities into memory.
     */
    @Scheduled(cron = "0 0 6 * * ?")
    @Transactional
    public void cleanOutDatedAppointments() {
        String cutoff = LocalDateTime.now().format(formatter);

        // Single DB query: DELETE WHERE paymentStatus=true AND serviceDateEnd < now
        // No records loaded into Java memory
        int deletedCount = appointmentRepository.deleteExpiredPaidAppointments(cutoff);

        if (deletedCount > 0) {
            log.info("Deleted {} outdated (paid) appointments before {}", deletedCount, cutoff);
        } else {
            log.debug("No outdated appointments to clean up at {}", cutoff);
        }
    }
}
