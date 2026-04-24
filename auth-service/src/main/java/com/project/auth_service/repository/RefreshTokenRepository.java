package com.project.auth_service.repository;

import com.project.auth_service.entity.RefreshToken;
import com.project.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findByUserAndRevokedFalse(User user);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.revoked = true OR r.expiresAt < :cutoff")
    int purgeStaleTokens(@Param("cutoff") Instant cutoff);
}
