package com.project.auth_service.job;

import com.project.auth_service.repository.RefreshTokenRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefreshTokenCleanupJobTest {

    private RefreshTokenCleanupJob cleanupJob;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        meterRegistry = new SimpleMeterRegistry();
        cleanupJob = new RefreshTokenCleanupJob(refreshTokenRepository, meterRegistry);
    }

    @Test
    void purgeExpiredTokens_ShouldCallRepositoryAndIncrementCounters() {
        when(refreshTokenRepository.purgeStaleTokens(any(Instant.class))).thenReturn(5);

        cleanupJob.purgeExpiredTokens();

        verify(refreshTokenRepository, times(1)).purgeStaleTokens(any(Instant.class));
        // Verify counters in SimpleMeterRegistry
        assertEquals(5.0, meterRegistry.counter("auth.tokens.cleanup.deleted.total").count());
        assertEquals(1.0, meterRegistry.get("auth.tokens.cleanup.status").tag("result", "success").counter().count());
    }

    @Test
    void purgeExpiredTokens_ShouldHandleException() {
        when(refreshTokenRepository.purgeStaleTokens(any(Instant.class))).thenThrow(new RuntimeException("DB error"));

        cleanupJob.purgeExpiredTokens();

        verify(refreshTokenRepository, times(1)).purgeStaleTokens(any(Instant.class));
        assertEquals(1.0, meterRegistry.get("auth.tokens.cleanup.status").tag("result", "failure").counter().count());
    }
}
