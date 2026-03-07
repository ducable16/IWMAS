package com.roamtrip.user.repository;

import com.roamtrip.user.entity.User;
import com.roamtrip.user.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);

    void deleteByUser(User user);
}
