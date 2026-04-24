package com.project.auth_service.job;

import com.project.auth_service.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Instant;

@Service
public class RefreshTokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final MeterRegistry meterRegistry;

    public RefreshTokenCleanupJob(RefreshTokenRepository refreshTokenRepository, MeterRegistry meterRegistry) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(cron = "0 0 3 * * ?") // Run every day at 3 AM
    @Transactional
    public void purgeExpiredTokens() {
        log.info("Starting refresh token cleanup job");
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            int deleted = refreshTokenRepository.purgeStaleTokens(Instant.now());
            log.info("Purged {} stale refresh tokens", deleted);
            meterRegistry.counter("auth.tokens.cleanup.deleted.total").increment(deleted);
            meterRegistry.counter("auth.tokens.cleanup.status", "result", "success").increment();
        } catch (Exception e) {
            log.error("Failed to purge stale refresh tokens", e);
            meterRegistry.counter("auth.tokens.cleanup.status", "result", "failure").increment();
        } finally {
            sample.stop(meterRegistry.timer("auth.tokens.cleanup.duration"));
        }
    }
}
