package com.iwas.user.mapper;

import com.iwas.common.storage.StorageService;
import com.iwas.user.dto.UserAdminView;
import com.iwas.user.dto.UserHRView;
import com.iwas.user.dto.UserMeResponse;
import com.iwas.user.dto.UserPublicView;
import com.iwas.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final StorageService storageService;

    public UserMeResponse toUserMeResponse(User user) {
        if (user == null) return null;
        return UserMeResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(storageService.resolveUrl(user.getAvatarUrl()))
                .position(user.getPosition())
                .role(user.getRole())
                .verified(user.getIsVerified())
                .active(user.getIsActive())
                .build();
    }

    public UserAdminView toAdminView(User user) {
        if (user == null) return null;
        return UserAdminView.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(storageService.resolveUrl(user.getAvatarUrl()))
                .position(user.getPosition())
                .role(user.getRole())
                .verified(user.getIsVerified())
                .active(user.getIsActive())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public UserHRView toHRView(User user) {
        if (user == null) return null;
        return UserHRView.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(storageService.resolveUrl(user.getAvatarUrl()))
                .position(user.getPosition())
                .role(user.getRole())
                .verified(user.getIsVerified())
                .active(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public UserPublicView toPublicView(User user) {
        if (user == null) return null;
        return UserPublicView.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(storageService.resolveUrl(user.getAvatarUrl()))
                .position(user.getPosition())
                .role(user.getRole())
                .build();
    }
}
