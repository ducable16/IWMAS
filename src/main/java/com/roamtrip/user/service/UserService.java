package com.roamtrip.user.service;

import com.roamtrip.auth.dto.ChangePasswordRequest;
import com.roamtrip.auth.dto.UpdateProfileRequest;
import com.roamtrip.auth.AuthService;
import com.roamtrip.common.enums.ErrorCode;
import com.roamtrip.common.exception.AppException;
import com.roamtrip.user.dto.CreateUserRequest;
import com.roamtrip.user.dto.UpdateUserRequest;
import com.roamtrip.user.dto.UserMeResponse;
import com.roamtrip.user.entity.User;
import com.roamtrip.user.enums.UserRole;
import com.roamtrip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public UserMeResponse getMe(Long userId) {
        return authService.toMeResponse(findUser(userId));
    }

    @Transactional
    public UserMeResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        if (request.getName() != null && !request.getName().isBlank()) {
            user.setFullName(request.getName().trim());
        }
        userRepository.save(user);
        return authService.toMeResponse(user);
    }

    @Transactional
    public String changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.WRONG_PASSWORD);
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return "Password changed";
    }

    @Transactional
    public UserMeResponse createUser(CreateUserRequest request) {
        userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .ifPresent(u -> { throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS); });

        User user = new User();
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setIsVerified(true);
        user.setRole(request.getRole() != null ? request.getRole() : UserRole.TEAM_MEMBER);
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user.setUsername(request.getUsername().trim());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getDepartmentId() != null) {
            user.setDepartmentId(request.getDepartmentId());
        }
        if (request.getPosition() != null && !request.getPosition().isBlank()) {
            user.setPosition(request.getPosition().trim());
        }
        return authService.toMeResponse(userRepository.save(user));
    }

    @Transactional
    public UserMeResponse updateUser(Long targetUserId, UpdateUserRequest request, String callerRole) {
        User user = findUser(targetUserId);

        if (request.getRole() != null && !"ADMIN".equals(callerRole)) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Only ADMIN can change user roles");
        }
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user.setUsername(request.getUsername().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().isBlank() ? null : request.getPhone().trim());
        }
        if (request.getDepartmentId() != null) {
            user.setDepartmentId(request.getDepartmentId());
        }
        if (request.getPosition() != null) {
            user.setPosition(request.getPosition().isBlank() ? null : request.getPosition().trim());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        return authService.toMeResponse(userRepository.save(user));
    }

    public List<UserMeResponse> getAllUsers() {
        return userRepository.findAllActiveUsers().stream()
                .map(authService::toMeResponse)
                .toList();
    }

    public UserMeResponse getUserById(Long userId) {
        return authService.toMeResponse(findUser(userId));
    }

    @Transactional
    public UserMeResponse toggleUserActive(Long userId, boolean active) {
        User user = findUser(userId);
        user.setIsActive(active);
        userRepository.save(user);
        return authService.toMeResponse(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
