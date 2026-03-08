package com.roamtrip.auth;

import com.roamtrip.auth.dto.ChangePasswordRequest;
import com.roamtrip.auth.dto.UpdateProfileRequest;
import com.roamtrip.user.dto.UserMeResponse;
import com.roamtrip.common.enums.ErrorCode;
import com.roamtrip.user.entity.User;
import com.roamtrip.common.exception.AppException;
import com.roamtrip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserMeResponse getMe(Long userId) {
        User user = findUser(userId);
        return toResponse(user);
    }

    public UserMeResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        if (request.getName() != null && !request.getName().isBlank()) {
            user.setFullName(request.getName().trim());
        }
        userRepository.save(user);
        return toResponse(user);
    }

    public String changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.WRONG_PASSWORD);
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return "Password changed";
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private UserMeResponse toResponse(User user) {
        return UserMeResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getFullName())
                .active(user.getIsActive())
                .build();
    }
}
