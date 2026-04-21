package com.iwas.auth.repository;

import com.iwas.auth.entity.PasswordReset;
import com.iwas.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {

    Optional<PasswordReset> findByResetTokenHash(String resetTokenHash);

    void deleteByUser(User user);
}
