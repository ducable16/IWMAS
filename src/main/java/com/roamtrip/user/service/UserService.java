package com.roamtrip.user.service;

import com.roamtrip.auth.dto.ChangePasswordRequest;
import com.roamtrip.auth.dto.UpdateProfileRequest;
import com.roamtrip.auth.AuthService;
import com.roamtrip.common.enums.ErrorCode;
import com.roamtrip.common.exception.AppException;
import com.roamtrip.user.dto.UserMeResponse;
import com.roamtrip.user.entity.User;
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
