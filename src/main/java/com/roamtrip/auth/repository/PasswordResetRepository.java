package com.roamtrip.auth.repository;

import com.roamtrip.auth.entity.PasswordReset;
import com.roamtrip.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {

    Optional<PasswordReset> findByResetTokenHash(String resetTokenHash);

    void deleteByUser(User user);
}
