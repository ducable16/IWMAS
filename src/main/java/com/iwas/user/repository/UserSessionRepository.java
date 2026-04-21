package com.iwas.user.repository;

import com.iwas.user.entity.User;
import com.iwas.user.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);

    void deleteByUser(User user);
}
