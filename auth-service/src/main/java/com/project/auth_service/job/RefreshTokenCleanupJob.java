package com.project.auth_service.job;

import com.project.auth_service.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RefreshTokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenCleanupJob(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Scheduled(cron = "0 0 3 * * ?") // Run every day at 3 AM
    @Transactional
    public void purgeExpiredTokens() {
        log.info("Starting refresh token cleanup job");
        try {
            int deleted = refreshTokenRepository.purgeStaleTokens(Instant.now());
            log.info("Purged {} stale refresh tokens", deleted);
        } catch (Exception e) {
            log.error("Failed to purge stale refresh tokens", e);
        }
    }
}
