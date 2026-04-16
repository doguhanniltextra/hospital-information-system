package com.project.auth_service.repository;

import com.project.auth_service.entity.RefreshToken;
import com.project.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findByUserAndRevokedFalse(User user);
}
