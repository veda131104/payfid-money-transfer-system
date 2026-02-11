package com.company.mts.repository;

import com.company.mts.entity.AuthToken;
import com.company.mts.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {
    Optional<AuthToken> findByToken(String token);
    List<AuthToken> findByUser(AuthUser user);
    List<AuthToken> findByUserAndIsValidTrue(AuthUser user);
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
