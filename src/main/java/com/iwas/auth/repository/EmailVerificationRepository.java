package com.iwas.auth.repository;

import com.iwas.auth.entity.EmailVerification;
import com.iwas.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByVerificationTokenHash(String verificationTokenHash);

    List<EmailVerification> findByUserOrderByCreatedAtDesc(User user);
}
