package com.iwas.user.service;

import com.iwas.auth.dto.ChangePasswordRequest;
import com.iwas.auth.dto.UpdateProfileRequest;
import com.iwas.auth.service.AuthService;
import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import com.iwas.common.storage.FileValidator;
import com.iwas.common.storage.StorageService;
import com.iwas.user.dto.AdminResetPasswordRequest;
import com.iwas.user.dto.CreateUserRequest;
import com.iwas.user.dto.UpdateUserRequest;
import com.iwas.user.dto.UserFilterRequest;
import com.iwas.user.dto.UserMeResponse;
import com.iwas.user.dto.UserPageResponse;
import com.iwas.user.entity.User;
import com.iwas.user.enums.UserRole;
import com.iwas.user.mapper.UserMapper;
import com.iwas.user.repository.UserRepository;
import com.iwas.user.repository.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final StorageService storageService;
    private final FileValidator fileValidator;
    private final UserMapper userMapper;

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
    public String adminResetPassword(Long targetUserId, AdminResetPasswordRequest request) {
        User user = findUser(targetUserId);
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return "Password reset successfully";
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
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone().trim());
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
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().isBlank() ? null : request.getPhone().trim());
        }
        if (request.getPosition() != null) {
            user.setPosition(request.getPosition().isBlank() ? null : request.getPosition().trim());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        return authService.toMeResponse(userRepository.save(user));
    }

    public UserPageResponse getAllUsers(UserFilterRequest filter, UserRole callerRole) {
        int size = Math.min(filter.getSize(), 100);
        Sort sort = buildSort(filter.getSortBy(), filter.getSortDirection());
        PageRequest pageRequest = PageRequest.of(filter.getPage(), size, sort);

        Page<User> page = userRepository.findAll(UserSpecification.fromFilter(filter), pageRequest);

        return UserPageResponse.builder()
                .content(page.getContent().stream().map(u -> mapByRole(u, callerRole)).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    private Sort buildSort(String sortBy, String direction) {
        Sort.Direction dir = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String field = switch (sortBy == null ? "" : sortBy.toLowerCase()) {
            case "email" -> "email";
            case "createdat", "created_at" -> "createdAt";
            case "lastloginat", "last_login_at" -> "lastLoginAt";
            default -> "fullName";
        };
        return Sort.by(dir, field);
    }

    public Object getUserById(Long userId, UserRole callerRole) {
        return mapByRole(findUser(userId), callerRole);
    }

    private Object mapByRole(User user, UserRole role) {
        return switch (role) {
            case ADMIN -> userMapper.toAdminView(user);
            case HR -> userMapper.toHRView(user);
            default -> userMapper.toPublicView(user);
        };
    }

    @Transactional
    public UserMeResponse toggleUserActive(Long userId, boolean active) {
        User user = findUser(userId);
        user.setIsActive(active);
        userRepository.save(user);
        return authService.toMeResponse(user);
    }

    @Transactional
    public UserMeResponse uploadAvatar(Long userId, MultipartFile file) {
        fileValidator.validateAvatar(file);
        User user = findUser(userId);
        String ext = resolveExtension(file.getOriginalFilename());
        String key = "avatars/" + userId + "/" + UUID.randomUUID() + ext;
        storageService.upload(file, key);
        user.setAvatarId(key);
        userRepository.save(user);
        return authService.toMeResponse(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private String resolveExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.'));
        }
        return "";
    }
}
