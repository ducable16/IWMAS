package com.roamtrip.repository;

import com.roamtrip.entity.user.EmailVerification;
import com.roamtrip.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByVerificationTokenHash(String verificationTokenHash);

    List<EmailVerification> findByUserOrderByCreatedAtDesc(User user);
}
