package com.roamtrip.auth.repository;

import com.roamtrip.auth.entity.OtpVerification;
import com.roamtrip.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByUserOrderByCreatedAtDesc(User user);

    void deleteByUser(User user);
}
