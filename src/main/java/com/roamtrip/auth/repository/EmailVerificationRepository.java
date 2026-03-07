package com.roamtrip.auth.repository;

import com.roamtrip.auth.entity.EmailVerification;
import com.roamtrip.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByVerificationTokenHash(String verificationTokenHash);

    List<EmailVerification> findByUserOrderByCreatedAtDesc(User user);
}
