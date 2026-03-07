package com.roamtrip.repository;

import com.roamtrip.entity.user.PasswordReset;
import com.roamtrip.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {

    Optional<PasswordReset> findByResetTokenHash(String resetTokenHash);

    void deleteByUser(User user);
}
