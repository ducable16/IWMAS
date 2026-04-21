package com.iwas.auth.repository;

import com.iwas.auth.entity.OtpVerification;
import com.iwas.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByUserOrderByCreatedAtDesc(User user);

    void deleteByUser(User user);
}
