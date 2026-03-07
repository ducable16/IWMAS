package com.roamtrip.repository;

import com.roamtrip.entity.user.User;
import com.roamtrip.entity.user.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);

    void deleteByUser(User user);
}
