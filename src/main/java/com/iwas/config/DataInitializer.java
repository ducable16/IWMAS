package com.iwas.config;

import com.iwas.user.entity.User;
import com.iwas.user.enums.UserRole;
import com.iwas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (!userRepository.existsByEmail("admin@workforce.com")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@workforce.com");
            admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
            admin.setFullName("System Admin");
            admin.setRole(UserRole.ADMIN);
            admin.setIsVerified(true);
            admin.setIsActive(true);
            admin.setIsDeleted(false);
            userRepository.save(admin);
            log.info("✅ Default admin account created: admin@workforce.com / Admin@123");
        } else {
            log.info("ℹ️ Admin account already exists, skipping seed.");
        }
    }
}
