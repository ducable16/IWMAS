package com.iwas.user.mapper;

import com.iwas.user.dto.UserAdminView;
import com.iwas.user.dto.UserHRView;
import com.iwas.user.dto.UserMeResponse;
import com.iwas.user.dto.UserPublicView;
import com.iwas.user.entity.User;

public final class UserMapper {

    private UserMapper() {}

    public static UserMeResponse toUserMeResponse(User user) {
        if (user == null) return null;
        return UserMeResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .position(user.getPosition())
                .role(user.getRole())
                .verified(user.getIsVerified())
                .active(user.getIsActive())
                .build();
    }

    public static UserAdminView toAdminView(User user) {
        if (user == null) return null;
        return UserAdminView.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .position(user.getPosition())
                .role(user.getRole())
                .verified(user.getIsVerified())
                .active(user.getIsActive())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public static UserHRView toHRView(User user) {
        if (user == null) return null;
        return UserHRView.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .position(user.getPosition())
                .role(user.getRole())
                .verified(user.getIsVerified())
                .active(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public static UserPublicView toPublicView(User user) {
        if (user == null) return null;
        return UserPublicView.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .position(user.getPosition())
                .role(user.getRole())
                .build();
    }
}
